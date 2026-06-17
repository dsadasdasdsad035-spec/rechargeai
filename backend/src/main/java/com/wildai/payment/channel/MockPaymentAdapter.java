package com.wildai.payment.channel;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Component
public class MockPaymentAdapter implements PaymentChannelAdapter {

    @Override
    public String channel() {
        return "MOCK";
    }

    @Override
    public Map<String, String> createPayment(String paymentNo, BigDecimal amount, String orderNo) {
        String mockTradeNo = "MOCK" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        return Map.of(
                "paymentNo", paymentNo,
                "codeUrl", "mock://pay/" + paymentNo,
                "mockTradeNo", mockTradeNo,
                "mockNotifyUrl", "/api/payments/mock/notify?paymentNo=" + paymentNo + "&tradeNo=" + mockTradeNo
        );
    }

    @Override
    public boolean verifyCallback(Map<String, String> params, String rawBody) {
        return params.containsKey("paymentNo");
    }

    @Override
    public String extractThirdTradeNo(Map<String, String> params) {
        return params.get("tradeNo");
    }
}
