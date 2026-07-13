package com.wildai.payment.dto;

import java.math.BigDecimal;

public record PaymentAmountSnapshot(
        BigDecimal orderAmount,
        String orderCurrency,
        BigDecimal paidAmount,
        String paidCurrency,
        BigDecimal exchangeRate
) {
    public BigDecimal settlementAmount() {
        return paidAmount != null ? paidAmount : orderAmount;
    }

    public String settlementCurrency() {
        return paidCurrency != null ? paidCurrency : orderCurrency;
    }
}
