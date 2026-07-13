package com.wildai.payment.service;

import com.wildai.order.domain.SubscriptionOrder;
import com.wildai.payment.domain.PaymentTransaction;
import com.wildai.payment.dto.PaymentAmountSnapshot;
import com.wildai.payment.repository.PaymentTransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class PaymentAmountService {

    private final PaymentTransactionRepository paymentRepo;

    public PaymentAmountService(PaymentTransactionRepository paymentRepo) {
        this.paymentRepo = paymentRepo;
    }

    public PaymentAmountSnapshot resolve(SubscriptionOrder order) {
        return paymentRepo.findByOrderId(order.getId())
                .map(payment -> fromPayment(order, payment))
                .orElseGet(() -> fromOrder(order));
    }

    private PaymentAmountSnapshot fromPayment(SubscriptionOrder order, PaymentTransaction payment) {
        BigDecimal orderAmount = payment.getOrderAmountDecimal() != null
                ? payment.getOrderAmountDecimal()
                : order.getAmount();
        String orderCurrency = notBlank(payment.getOrderCurrency())
                ? normalizeCurrency(payment.getOrderCurrency())
                : normalizeCurrency(order.getCurrency());
        return new PaymentAmountSnapshot(
                orderAmount,
                orderCurrency,
                payment.getAmountDecimal(),
                normalizeCurrency(payment.getCurrency()),
                payment.getExchangeRateDecimal()
        );
    }

    private PaymentAmountSnapshot fromOrder(SubscriptionOrder order) {
        return new PaymentAmountSnapshot(
                order.getAmount(),
                normalizeCurrency(order.getCurrency()),
                null,
                null,
                null
        );
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private String normalizeCurrency(String currency) {
        return currency == null || currency.isBlank() ? "CNY" : currency.trim().toUpperCase();
    }
}
