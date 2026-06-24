package com.wildai.payment.channel;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Component
public class WechatPaymentAdapter implements PaymentChannelAdapter {

    @Override
    public String channel() {
        return "WECHAT";
    }

    @Override
    public Map<String, String> createPayment(String paymentNo, BigDecimal amount, String orderNo) {
        // 一期占位：生产环境接入微信 Native 支付
        return Map.of("paymentNo", paymentNo, "codeUrl", "weixin://wxpay/bizpayurl?pr=" + paymentNo);
    }

    @Override
    public boolean verifyCallback(Map<String, String> params, String rawBody) {
        return true;
    }

    @Override
    public String extractThirdTradeNo(Map<String, String> params) {
        return params.getOrDefault("transaction_id", "WX" + UUID.randomUUID().toString().substring(0, 12));
    }

    @Override
    public ChannelRefundResult refund(String paymentNo, String thirdTradeNo, String refundNo, BigDecimal amount) {
        return ChannelRefundResult.ok("WXRF" + UUID.randomUUID().toString().substring(0, 12));
    }
}
