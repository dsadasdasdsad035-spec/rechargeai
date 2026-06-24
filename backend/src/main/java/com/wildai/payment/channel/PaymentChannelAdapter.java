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

    /**
     * 发起渠道全额退款；默认不支持，各渠道适配器按需实现。
     */
    default ChannelRefundResult refund(String paymentNo, String thirdTradeNo, String refundNo, BigDecimal amount) {
        return ChannelRefundResult.fail("渠道暂不支持退款");
    }
}
