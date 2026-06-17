package com.wildai.payment.channel;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Component
public class AlipayPaymentAdapter implements PaymentChannelAdapter {

    @Override
    public String channel() {
        return "ALIPAY";
    }

    @Override
    public Map<String, String> createPayment(String paymentNo, BigDecimal amount, String orderNo) {
        return Map.of("paymentNo", paymentNo, "codeUrl", "https://openapi.alipay.com/mock/" + paymentNo);
    }

    @Override
    public boolean verifyCallback(Map<String, String> params, String rawBody) {
        return true;
    }

    @Override
    public String extractThirdTradeNo(Map<String, String> params) {
        return params.getOrDefault("trade_no", "ALI" + UUID.randomUUID().toString().substring(0, 12));
    }
}
