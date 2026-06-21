package com.wildai.payment.service;

import com.wildai.common.config.WildAiProperties;
import com.wildai.common.exception.BusinessException;
import com.wildai.order.domain.SubscriptionOrder;
import com.wildai.order.repository.SubscriptionOrderRepository;
import com.wildai.order.service.OrderService;
import com.wildai.payment.channel.PaymentChannelAdapter;
import com.wildai.payment.domain.PaymentTransaction;
import com.wildai.payment.repository.PaymentTransactionRepository;
import org.junit.jupiter.api.Test;

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

class PaymentServiceTest {

    private final PaymentTransactionRepository paymentRepo = mock(PaymentTransactionRepository.class);
    private final SubscriptionOrderRepository orderRepo = mock(SubscriptionOrderRepository.class);
    private final OrderService orderService = mock(OrderService.class);
    private final XunhuPayCallbackAdapter adapter = new XunhuPayCallbackAdapter();
    private final PaymentService paymentService = new PaymentService(
            paymentRepo,
            orderRepo,
            orderService,
            new WildAiProperties(),
            List.of(adapter)
    );

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
        SubscriptionOrder order = new SubscriptionOrder();
        order.setId(42L);
        order.setAmount(amount);
        order.setOrderNo("O1001");
        return order;
    }

    private static class XunhuPayCallbackAdapter implements PaymentChannelAdapter {

        @Override
        public String channel() {
            return "XUNHUPAY";
        }

        @Override
        public Map<String, String> createPayment(String paymentNo, BigDecimal amount, String orderNo) {
            return Map.of();
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
