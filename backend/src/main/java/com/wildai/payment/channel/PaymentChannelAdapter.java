package com.wildai.payment.channel;

import java.math.BigDecimal;
import java.util.Map;

public interface PaymentChannelAdapter {
    String channel();
    Map<String, String> createPayment(String paymentNo, BigDecimal amount, String orderNo);
    boolean verifyCallback(Map<String, String> params, String rawBody);
    String extractThirdTradeNo(Map<String, String> params);

    default String extractPaymentNo(Map<String, String> params) {
        return params.get("paymentNo");
    }

    default BigDecimal extractPaidAmount(Map<String, String> params) {
        return null;
    }

    default boolean isPaidCallback(Map<String, String> params) {
        return true;
    }
}
