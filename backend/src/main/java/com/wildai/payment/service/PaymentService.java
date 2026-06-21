package com.wildai.payment.service;

import com.wildai.common.config.WildAiProperties;
import com.wildai.common.exception.BusinessException;
import com.wildai.common.exception.ErrorCode;
import com.wildai.order.domain.SubscriptionOrder;
import com.wildai.order.repository.SubscriptionOrderRepository;
import com.wildai.order.service.OrderService;
import com.wildai.payment.channel.PaymentChannelAdapter;
import com.wildai.payment.domain.PaymentTransaction;
import com.wildai.payment.repository.PaymentTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PaymentService {

    private final PaymentTransactionRepository paymentRepo;
    private final SubscriptionOrderRepository orderRepo;
    private final OrderService orderService;
    private final WildAiProperties properties;
    private final Map<String, PaymentChannelAdapter> adapters;

    public PaymentService(PaymentTransactionRepository paymentRepo, SubscriptionOrderRepository orderRepo,
                          OrderService orderService, WildAiProperties properties,
                          List<PaymentChannelAdapter> adapterList) {
        this.paymentRepo = paymentRepo;
        this.orderRepo = orderRepo;
        this.orderService = orderService;
        this.properties = properties;
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
            p.setAmount(order.getAmount());
            p.setStatus("PAYING");
            return paymentRepo.save(p);
        });

        if (!"PAID".equals(payment.getStatus())) {
            payment.setStatus("PAYING");
            payment.setChannel(effectiveChannel);
            paymentRepo.save(payment);
            order.setPaymentStatus("PAYING");
            orderRepo.save(order);
        }

        return adapter.createPayment(payment.getPaymentNo(), payment.getAmountDecimal(), orderNo);
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

        if (payment.getAmountDecimal().compareTo(order.getAmount()) != 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "支付金额不一致");
        }
        BigDecimal paidAmount = adapter.extractPaidAmount(params);
        if (paidAmount != null && paidAmount.compareTo(order.getAmount()) != 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "支付金额不一致");
        }

        payment.setStatus("PAID");
        payment.setThirdTradeNo(thirdTradeNo);
        payment.setCallbackVerified(true);
        payment.setCallbackRawRedacted("***");
        payment.setPaidAt(Instant.now());
        paymentRepo.save(payment);

        orderService.markPaid(order);
    }
}
