package com.wildai.payment.service;

import com.wildai.common.config.WildAiProperties;
import com.wildai.common.exception.BusinessException;
import com.wildai.common.exception.ErrorCode;
import com.wildai.order.domain.SubscriptionOrder;
import com.wildai.order.repository.SubscriptionOrderRepository;
import com.wildai.order.service.OrderService;
import com.wildai.payment.channel.PaymentChannelAdapter;
import com.wildai.payment.domain.PaymentTransaction;
import com.wildai.payment.event.OrderPaidEvent;
import com.wildai.payment.repository.PaymentTransactionRepository;
import com.wildai.setting.service.SystemSettingService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PaymentService {

    private static final String CHANNEL_CURRENCY = "CNY";
    private static final int MONEY_SCALE = 2;
    private static final int RATE_SCALE = 6;

    private final PaymentTransactionRepository paymentRepo;
    private final SubscriptionOrderRepository orderRepo;
    private final OrderService orderService;
    private final WildAiProperties properties;
    private final SystemSettingService settingService;
    private final ApplicationEventPublisher eventPublisher;
    private final Map<String, PaymentChannelAdapter> adapters;

    public PaymentService(PaymentTransactionRepository paymentRepo, SubscriptionOrderRepository orderRepo,
                          OrderService orderService, WildAiProperties properties,
                          SystemSettingService settingService,
                          ApplicationEventPublisher eventPublisher,
                          List<PaymentChannelAdapter> adapterList) {
        this.paymentRepo = paymentRepo;
        this.orderRepo = orderRepo;
        this.orderService = orderService;
        this.properties = properties;
        this.settingService = settingService;
        this.eventPublisher = eventPublisher;
        this.adapters = adapterList.stream()
                .collect(Collectors.toMap(PaymentChannelAdapter::channel, Function.identity()));
    }

    @Transactional
    public Map<String, String> createPayment(String orderNo, Long userId, String channel) {
        SubscriptionOrder order = orderService.requireOwned(orderNo, userId);
        if (!"WAIT_PAY".equals(order.getOrderStatus())) {
            throw new BusinessException(ErrorCode.ORDER_NOT_PAYABLE);
        }
        if (order.getExpiredAt() != null && Instant.now().isAfter(order.getExpiredAt())) {
            throw new BusinessException(ErrorCode.ORDER_NOT_PAYABLE, "订单已过期");
        }

        String effectiveChannel = properties.getPayment().isMockEnabled() ? "MOCK" : channel;
        PaymentChannelAdapter adapter = adapters.get(effectiveChannel);
        if (adapter == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的支付渠道");
        }

        PaymentTransaction payment = paymentRepo.findByOrderId(order.getId()).orElseGet(() -> {
            PaymentTransaction p = new PaymentTransaction();
            p.setPaymentNo("P" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 4).toUpperCase());
            p.setOrderId(order.getId());
            p.setChannel(effectiveChannel);
            p.setStatus("PAYING");
            return p;
        });

        if (!"PAID".equals(payment.getStatus())) {
            payment.setStatus("PAYING");
            payment.setChannel(effectiveChannel);
            if (!hasLockedPaymentAmount(payment, order)) {
                ChannelPaymentAmount channelAmount = resolveChannelPaymentAmount(order);
                applyPaymentAmount(payment, order, channelAmount);
            }
            paymentRepo.save(payment);
            order.setPaymentStatus("PAYING");
            orderRepo.save(order);
        }

        Map<String, String> payInfo = new LinkedHashMap<>(
                adapter.createPayment(payment.getPaymentNo(), payment.getAmountDecimal(), orderNo));
        payInfo.put("amount", money(payment.getAmountDecimal()));
        payInfo.put("currency", normalizeCurrency(payment.getCurrency()));
        payInfo.put("orderAmount", money(resolveOrderAmount(payment, order)));
        payInfo.put("orderCurrency", resolveOrderCurrency(payment, order));
        if (payment.getExchangeRateDecimal() != null) {
            payInfo.put("exchangeRate", rate(payment.getExchangeRateDecimal()));
        }
        return payInfo;
    }

    @Transactional
    public void handleCallback(String channel, Map<String, String> params, String rawBody) {
        PaymentChannelAdapter adapter = adapters.get(channel);
        if (adapter == null || !adapter.verifyCallback(params, rawBody)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "回调验签失败");
        }
        if (!adapter.isPaidCallback(params)) {
            return;
        }

        String thirdTradeNo = adapter.extractThirdTradeNo(params);
        if (thirdTradeNo != null) {
            var paid = paymentRepo.findByChannelAndThirdTradeNo(channel, thirdTradeNo);
            if (paid.isPresent() && "PAID".equals(paid.get().getStatus())) {
                return;
            }
        }

        String paymentNo = adapter.extractPaymentNo(params);
        if (paymentNo == null || paymentNo.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "支付单号缺失");
        }
        PaymentTransaction payment = paymentRepo.findByPaymentNo(paymentNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "支付单不存在"));

        if ("PAID".equals(payment.getStatus())) {
            return;
        }

        SubscriptionOrder order = orderRepo.findById(payment.getOrderId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        BigDecimal paidAmount = adapter.extractPaidAmount(params);
        if (paidAmount != null && paidAmount.compareTo(payment.getAmountDecimal()) != 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "支付金额不一致");
        }

        payment.setStatus("PAID");
        payment.setThirdTradeNo(thirdTradeNo);
        payment.setCallbackVerified(true);
        payment.setCallbackRawRedacted("***");
        payment.setPaidAt(Instant.now());
        paymentRepo.save(payment);

        orderService.markPaid(order);
        eventPublisher.publishEvent(new OrderPaidEvent(order, payment));
    }

    private ChannelPaymentAmount resolveChannelPaymentAmount(SubscriptionOrder order) {
        String orderCurrency = normalizeCurrency(order.getCurrency());
        BigDecimal orderAmount = order.getAmount();
        if (orderAmount == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "订单金额缺失");
        }
        if (CHANNEL_CURRENCY.equals(orderCurrency)) {
            return new ChannelPaymentAmount(orderAmount.setScale(MONEY_SCALE, RoundingMode.HALF_UP), CHANNEL_CURRENCY, null);
        }
        if ("USD".equals(orderCurrency)) {
            BigDecimal exchangeRate = usdToCnyRate();
            BigDecimal channelAmount = orderAmount.multiply(exchangeRate).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
            return new ChannelPaymentAmount(channelAmount, CHANNEL_CURRENCY, exchangeRate);
        }
        throw new BusinessException(ErrorCode.BAD_REQUEST, "当前支付渠道暂不支持币种：" + orderCurrency);
    }

    private void applyPaymentAmount(PaymentTransaction payment, SubscriptionOrder order, ChannelPaymentAmount channelAmount) {
        payment.setAmount(channelAmount.amount());
        payment.setCurrency(channelAmount.currency());
        payment.setOrderAmount(order.getAmount().setScale(MONEY_SCALE, RoundingMode.HALF_UP));
        payment.setOrderCurrency(normalizeCurrency(order.getCurrency()));
        payment.setExchangeRate(channelAmount.exchangeRate());
    }

    private BigDecimal usdToCnyRate() {
        BigDecimal rate = settingService.getUsdToCnyRate();
        if (rate == null || rate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "美元兑人民币汇率配置无效");
        }
        return rate.setScale(RATE_SCALE, RoundingMode.HALF_UP);
    }

    private boolean hasLockedPaymentAmount(PaymentTransaction payment, SubscriptionOrder order) {
        if (payment.getAmountDecimal() == null || payment.getCurrency() == null || payment.getCurrency().isBlank()) {
            return false;
        }
        String orderCurrency = normalizeCurrency(order.getCurrency());
        if (payment.getOrderAmountDecimal() == null || payment.getOrderCurrency() == null || payment.getOrderCurrency().isBlank()) {
            return false;
        }
        if (!orderCurrency.equals(normalizeCurrency(payment.getOrderCurrency()))) {
            return false;
        }
        return CHANNEL_CURRENCY.equals(orderCurrency) || payment.getExchangeRateDecimal() != null;
    }

    private BigDecimal resolveOrderAmount(PaymentTransaction payment, SubscriptionOrder order) {
        return payment.getOrderAmountDecimal() != null ? payment.getOrderAmountDecimal() : order.getAmount();
    }

    private String resolveOrderCurrency(PaymentTransaction payment, SubscriptionOrder order) {
        return payment.getOrderCurrency() != null ? normalizeCurrency(payment.getOrderCurrency()) : normalizeCurrency(order.getCurrency());
    }

    private String normalizeCurrency(String currency) {
        return currency == null || currency.isBlank() ? CHANNEL_CURRENCY : currency.trim().toUpperCase();
    }

    private String money(BigDecimal amount) {
        return amount.setScale(MONEY_SCALE, RoundingMode.HALF_UP).toPlainString();
    }

    private String rate(BigDecimal amount) {
        return amount.setScale(RATE_SCALE, RoundingMode.HALF_UP).toPlainString();
    }

    private record ChannelPaymentAmount(BigDecimal amount, String currency, BigDecimal exchangeRate) {
    }
}
