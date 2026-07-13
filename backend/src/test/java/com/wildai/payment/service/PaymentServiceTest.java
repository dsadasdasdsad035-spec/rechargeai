package com.wildai.payment.service;

import com.wildai.common.config.WildAiProperties;
import com.wildai.common.exception.BusinessException;
import com.wildai.order.domain.SubscriptionOrder;
import com.wildai.order.repository.SubscriptionOrderRepository;
import com.wildai.order.service.OrderService;
import com.wildai.payment.channel.PaymentChannelAdapter;
import com.wildai.payment.domain.PaymentTransaction;
import com.wildai.payment.event.OrderPaidEvent;
import com.wildai.payment.repository.PaymentTransactionRepository;
import com.wildai.setting.service.SystemSettingService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.any;

class PaymentServiceTest {

    private final PaymentTransactionRepository paymentRepo = mock(PaymentTransactionRepository.class);
    private final SubscriptionOrderRepository orderRepo = mock(SubscriptionOrderRepository.class);
    private final OrderService orderService = mock(OrderService.class);
    private final SystemSettingService settingService = mock(SystemSettingService.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final XunhuPayCallbackAdapter adapter = new XunhuPayCallbackAdapter();
    private final WildAiProperties properties = properties();
    private final PaymentService paymentService = new PaymentService(
            paymentRepo,
            orderRepo,
            orderService,
            properties,
            settingService,
            eventPublisher,
            List.of(adapter)
    );

    @Test
    void createPaymentConvertsUsdOrderToCnyChannelAmountFromDynamicRate() {
        PaymentTransaction saved = new PaymentTransaction();
        SubscriptionOrder order = order(new BigDecimal("25.99"), "USD");
        when(orderService.requireOwned("O1001", 7L)).thenReturn(order);
        when(settingService.getUsdToCnyRate()).thenReturn(new BigDecimal("7.500000"));
        when(paymentRepo.findByOrderId(42L)).thenReturn(Optional.empty());
        when(paymentRepo.save(any(PaymentTransaction.class))).thenAnswer(invocation -> {
            PaymentTransaction payment = invocation.getArgument(0);
            payment.setId(99L);
            saved.setPaymentNo(payment.getPaymentNo());
            saved.setOrderId(payment.getOrderId());
            saved.setChannel(payment.getChannel());
            saved.setAmount(payment.getAmountDecimal());
            saved.setStatus(payment.getStatus());
            saved.setCurrency(payment.getCurrency());
            saved.setOrderAmount(payment.getOrderAmountDecimal());
            saved.setOrderCurrency(payment.getOrderCurrency());
            saved.setExchangeRate(payment.getExchangeRateDecimal());
            return payment;
        });

        Map<String, String> payInfo = paymentService.createPayment("O1001", 7L, "XUNHUPAY");

        assertThat(adapter.createdAmount).isEqualByComparingTo("194.93");
        assertThat(saved.getAmountDecimal()).isEqualByComparingTo("194.93");
        assertThat(saved.getCurrency()).isEqualTo("CNY");
        assertThat(saved.getOrderAmountDecimal()).isEqualByComparingTo("25.99");
        assertThat(saved.getOrderCurrency()).isEqualTo("USD");
        assertThat(saved.getExchangeRateDecimal()).isEqualByComparingTo("7.500000");
        assertThat(payInfo).containsEntry("amount", "194.93");
        assertThat(payInfo).containsEntry("currency", "CNY");
        assertThat(payInfo).containsEntry("orderAmount", "25.99");
        assertThat(payInfo).containsEntry("orderCurrency", "USD");
        assertThat(payInfo).containsEntry("exchangeRate", "7.500000");
    }

    @Test
    void createPaymentKeepsExistingLockedRateAndChannelAmount() {
        SubscriptionOrder order = order(new BigDecimal("25.99"), "USD");
        PaymentTransaction existing = payment("P1001", new BigDecimal("188.43"));
        existing.setCurrency("CNY");
        existing.setOrderAmount(new BigDecimal("25.99"));
        existing.setOrderCurrency("USD");
        existing.setExchangeRate(new BigDecimal("7.250000"));
        when(orderService.requireOwned("O1001", 7L)).thenReturn(order);
        when(paymentRepo.findByOrderId(42L)).thenReturn(Optional.of(existing));
        when(paymentRepo.save(any(PaymentTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, String> payInfo = paymentService.createPayment("O1001", 7L, "XUNHUPAY");

        assertThat(adapter.createdAmount).isEqualByComparingTo("188.43");
        assertThat(existing.getAmountDecimal()).isEqualByComparingTo("188.43");
        assertThat(existing.getExchangeRateDecimal()).isEqualByComparingTo("7.250000");
        assertThat(payInfo).containsEntry("amount", "188.43");
        assertThat(payInfo).containsEntry("exchangeRate", "7.250000");
        verify(settingService, never()).getUsdToCnyRate();
    }

    @Test
    void handleCallbackUsesAdapterPaymentNoAndMarksOrderPaid() {
        PaymentTransaction payment = payment("P1001", new BigDecimal("12.30"));
        SubscriptionOrder order = order(new BigDecimal("12.30"));
        when(paymentRepo.findByChannelAndThirdTradeNo("XUNHUPAY", "TX1001")).thenReturn(Optional.empty());
        when(paymentRepo.findByPaymentNo("P1001")).thenReturn(Optional.of(payment));
        when(orderRepo.findById(42L)).thenReturn(Optional.of(order));

        paymentService.handleCallback("XUNHUPAY", Map.of(
                "trade_order_id", "P1001",
                "transaction_id", "TX1001",
                "total_fee", "12.30",
                "status", "OD"
        ), null);

        assertThat(payment.getStatus()).isEqualTo("PAID");
        assertThat(payment.getThirdTradeNo()).isEqualTo("TX1001");
        assertThat(payment.getCallbackVerified()).isTrue();
        verify(paymentRepo).save(payment);
        verify(orderService).markPaid(order);
        ArgumentCaptor<OrderPaidEvent> eventCaptor = ArgumentCaptor.forClass(OrderPaidEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().order()).isSameAs(order);
        assertThat(eventCaptor.getValue().payment()).isSameAs(payment);
    }

    @Test
    void handleCallbackComparesPaidAmountWithChannelAmountForUsdOrder() {
        PaymentTransaction payment = payment("P1001", new BigDecimal("188.43"));
        SubscriptionOrder order = order(new BigDecimal("25.99"), "USD");
        when(paymentRepo.findByChannelAndThirdTradeNo("XUNHUPAY", "TX1001")).thenReturn(Optional.empty());
        when(paymentRepo.findByPaymentNo("P1001")).thenReturn(Optional.of(payment));
        when(orderRepo.findById(42L)).thenReturn(Optional.of(order));

        paymentService.handleCallback("XUNHUPAY", Map.of(
                "trade_order_id", "P1001",
                "transaction_id", "TX1001",
                "total_fee", "188.43",
                "status", "OD"
        ), null);

        assertThat(payment.getStatus()).isEqualTo("PAID");
        verify(orderService).markPaid(order);
    }

    @Test
    void handleCallbackRejectsMismatchedCallbackAmount() {
        PaymentTransaction payment = payment("P1001", new BigDecimal("12.30"));
        SubscriptionOrder order = order(new BigDecimal("12.30"));
        when(paymentRepo.findByChannelAndThirdTradeNo("XUNHUPAY", "TX1001")).thenReturn(Optional.empty());
        when(paymentRepo.findByPaymentNo("P1001")).thenReturn(Optional.of(payment));
        when(orderRepo.findById(42L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> paymentService.handleCallback("XUNHUPAY", Map.of(
                "trade_order_id", "P1001",
                "transaction_id", "TX1001",
                "total_fee", "13.30",
                "status", "OD"
        ), null)).isInstanceOf(BusinessException.class)
                .hasMessage("支付金额不一致");

        assertThat(payment.getStatus()).isEqualTo("PAYING");
        verify(paymentRepo, never()).save(payment);
        verify(orderService, never()).markPaid(order);
    }

    private PaymentTransaction payment(String paymentNo, BigDecimal amount) {
        PaymentTransaction payment = new PaymentTransaction();
        payment.setPaymentNo(paymentNo);
        payment.setOrderId(42L);
        payment.setChannel("XUNHUPAY");
        payment.setAmount(amount);
        payment.setStatus("PAYING");
        return payment;
    }

    private SubscriptionOrder order(BigDecimal amount) {
        return order(amount, "CNY");
    }

    private SubscriptionOrder order(BigDecimal amount, String currency) {
        SubscriptionOrder order = new SubscriptionOrder();
        order.setId(42L);
        order.setAmount(amount);
        order.setCurrency(currency);
        order.setOrderNo("O1001");
        order.setOrderStatus("WAIT_PAY");
        order.setPaymentStatus("UNPAID");
        return order;
    }

    private WildAiProperties properties() {
        WildAiProperties properties = new WildAiProperties();
        properties.getPayment().setMockEnabled(false);
        return properties;
    }

    private static class XunhuPayCallbackAdapter implements PaymentChannelAdapter {

        private BigDecimal createdAmount;

        @Override
        public String channel() {
            return "XUNHUPAY";
        }

        @Override
        public Map<String, String> createPayment(String paymentNo, BigDecimal amount, String orderNo) {
            this.createdAmount = amount;
            return Map.of("paymentNo", paymentNo, "codeUrl", "mock://pay/" + paymentNo);
        }

        @Override
        public boolean verifyCallback(Map<String, String> params, String rawBody) {
            return true;
        }

        @Override
        public String extractPaymentNo(Map<String, String> params) {
            return params.get("trade_order_id");
        }

        @Override
        public String extractThirdTradeNo(Map<String, String> params) {
            return params.get("transaction_id");
        }

        @Override
        public BigDecimal extractPaidAmount(Map<String, String> params) {
            return new BigDecimal(params.get("total_fee"));
        }

        @Override
        public boolean isPaidCallback(Map<String, String> params) {
            return "OD".equals(params.get("status"));
        }
    }
}
