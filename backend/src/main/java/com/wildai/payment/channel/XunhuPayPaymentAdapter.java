package com.wildai.payment.channel;

import com.wildai.common.config.WildAiProperties;
import com.wildai.common.exception.BusinessException;
import com.wildai.common.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

@Component
public class XunhuPayPaymentAdapter implements PaymentChannelAdapter {

    private static final String CHANNEL = "XUNHUPAY";

    private final WildAiProperties properties;
    private final RestTemplate restTemplate;
    private final Clock clock;
    private final Supplier<String> nonceSupplier;

    @Autowired
    public XunhuPayPaymentAdapter(WildAiProperties properties, RestTemplateBuilder restTemplateBuilder) {
        this(properties,
                restTemplateBuilder
                        .setConnectTimeout(Duration.ofSeconds(properties.getPayment().getXunhupay().getTimeoutSeconds()))
                        .setReadTimeout(Duration.ofSeconds(properties.getPayment().getXunhupay().getTimeoutSeconds()))
                        .build(),
                Clock.systemUTC(),
                XunhuPayPaymentAdapter::newNonce);
    }

    XunhuPayPaymentAdapter(WildAiProperties properties, RestTemplate restTemplate, Clock clock, Supplier<String> nonceSupplier) {
        this.properties = properties;
        this.restTemplate = restTemplate;
        this.clock = clock;
        this.nonceSupplier = nonceSupplier;
    }

    @Override
    public String channel() {
        return CHANNEL;
    }

    @Override
    public Map<String, String> createPayment(String paymentNo, BigDecimal amount, String orderNo) {
        WildAiProperties.Payment.XunhuPay config = config();
        requireText(config.getAppid(), "虎皮椒 APPID 未配置");
        requireText(config.getSecret(), "虎皮椒密钥未配置");
        requireText(config.getNotifyUrl(), "虎皮椒异步回调地址未配置");

        Map<String, String> params = new LinkedHashMap<>();
        params.put("version", "1.1");
        params.put("appid", config.getAppid());
        params.put("trade_order_id", paymentNo);
        params.put("total_fee", amount.setScale(2, RoundingMode.HALF_UP).toPlainString());
        params.put("title", "RechargeAi 订单 " + orderNo);
        params.put("notify_url", config.getNotifyUrl());
        params.put("return_url", config.getReturnUrl());
        params.put("callback_url", config.getCallbackUrl());
        params.put("time", String.valueOf(clock.instant().getEpochSecond()));
        params.put("nonce_str", nonceSupplier.get());
        params.put("hash", XunhuPaySigner.sign(params, config.getSecret()));

        Map<String, Object> response = postCreatePayment(config, params);
        Object errcode = response.get("errcode");
        if (!"0".equals(String.valueOf(errcode))) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "虎皮椒发起支付失败：" + response.getOrDefault("errmsg", "未知错误"));
        }

        Map<?, ?> data = responseData(response);
        String openOrderId = stringValue(data.get("open_order_id"));
        if (openOrderId.isBlank()) {
            openOrderId = stringValue(data.get("openid"));
        }
        return Map.of(
                "paymentNo", paymentNo,
                "paymentUrl", stringValue(data.get("url")),
                "codeUrl", stringValue(data.get("url_qrcode")),
                "openOrderId", openOrderId
        );
    }

    @Override
    public boolean verifyCallback(Map<String, String> params, String rawBody) {
        WildAiProperties.Payment.XunhuPay config = config();
        requireText(config.getSecret(), "虎皮椒密钥未配置");
        if (!XunhuPaySigner.verify(params, config.getSecret())) {
            return false;
        }
        String appid = params.get("appid");
        return appid == null || appid.equals(config.getAppid());
    }

    @Override
    public String extractPaymentNo(Map<String, String> params) {
        return params.get("trade_order_id");
    }

    @Override
    public String extractThirdTradeNo(Map<String, String> params) {
        String transactionId = params.get("transaction_id");
        if (transactionId != null && !transactionId.isBlank()) {
            return transactionId;
        }
        return params.get("open_order_id");
    }

    @Override
    public BigDecimal extractPaidAmount(Map<String, String> params) {
        String totalFee = params.get("total_fee");
        return totalFee == null || totalFee.isBlank() ? null : new BigDecimal(totalFee);
    }

    @Override
    public boolean isPaidCallback(Map<String, String> params) {
        return "OD".equals(params.get("status"));
    }

    @Override
    public ChannelRefundResult refund(String paymentNo, String thirdTradeNo, String refundNo, BigDecimal amount) {
        WildAiProperties.Payment.XunhuPay config = config();
        requireText(config.getAppid(), "虎皮椒 APPID 未配置");
        requireText(config.getSecret(), "虎皮椒密钥未配置");

        boolean hasPaymentNo = paymentNo != null && !paymentNo.isBlank();
        boolean hasThirdTradeNo = thirdTradeNo != null && !thirdTradeNo.isBlank();
        if (!hasPaymentNo && !hasThirdTradeNo) {
            return ChannelRefundResult.fail("缺少商户支付单号或虎皮椒订单号");
        }

        Map<String, String> params = new LinkedHashMap<>();
        params.put("appid", config.getAppid());
        params.put("reason", "RechargeAi 退款 " + refundNo);
        params.put("time", String.valueOf(clock.instant().getEpochSecond()));
        params.put("nonce_str", nonceSupplier.get());
        if (hasPaymentNo) {
            params.put("trade_order_id", paymentNo);
        } else {
            params.put("open_order_id", thirdTradeNo);
        }
        params.put("hash", XunhuPaySigner.sign(params, config.getSecret()));

        Map<String, Object> response;
        try {
            response = postJson(config, "/payment/refund.html", params);
        } catch (BusinessException e) {
            return ChannelRefundResult.fail(e.getMessage());
        }

        Object errcode = response.get("errcode");
        if (!"0".equals(String.valueOf(errcode))) {
            Object errmsg = response.get("errmsg");
            return ChannelRefundResult.fail("虎皮椒退款失败：" + (errmsg != null ? errmsg : "未知错误"));
        }

        if (!verifyResponseHash(response, config.getSecret())) {
            return ChannelRefundResult.fail("虎皮椒退款响应验签失败");
        }

        String refundStatus = stringValue(response.get("refund_status"));
        return switch (refundStatus) {
            case "CD" -> ChannelRefundResult.ok(resolveChannelRefundNo(response));
            case "RD" -> ChannelRefundResult.fail("虎皮椒退款处理中，请稍后重试");
            case "OD" -> ChannelRefundResult.fail("虎皮椒订单仍为已支付状态，退款未受理");
            default -> ChannelRefundResult.fail("虎皮椒返回未知退款状态: " + refundStatus);
        };
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> postJson(WildAiProperties.Payment.XunhuPay config, String path, Map<String, String> params) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/json;charset=UTF-8"));
            Map<String, Object> response = restTemplate.postForObject(
                    endpoint(config, path), new HttpEntity<>(params, headers), Map.class);
            if (response == null) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "虎皮椒网关返回为空");
            }
            return response;
        } catch (BusinessException e) {
            throw e;
        } catch (RestClientException e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "虎皮椒网关请求失败");
        }
    }

    private Map<String, Object> postCreatePayment(WildAiProperties.Payment.XunhuPay config, Map<String, String> params) {
        return postJson(config, "/payment/do.html", params);
    }

    private boolean verifyResponseHash(Map<String, Object> response, String secret) {
        Map<String, String> signed = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : response.entrySet()) {
            if (entry.getValue() != null) {
                signed.put(entry.getKey(), String.valueOf(entry.getValue()));
            }
        }
        return XunhuPaySigner.verify(signed, secret);
    }

    private String resolveChannelRefundNo(Map<String, Object> response) {
        String outRefundNo = stringValue(response.get("out_refund_no"));
        if (!outRefundNo.isBlank()) {
            return outRefundNo;
        }
        String transactionId = stringValue(response.get("transaction_id"));
        if (!transactionId.isBlank()) {
            return transactionId;
        }
        return stringValue(response.get("trade_order_id"));
    }

    private Map<?, ?> responseData(Map<String, Object> response) {
        Object data = response.get("data");
        if (data instanceof Map<?, ?> map) {
            return map;
        }
        if (response.get("url") != null || response.get("url_qrcode") != null) {
            return response;
        }
        throw new BusinessException(ErrorCode.BAD_REQUEST, "虎皮椒支付网关返回格式异常");
    }

    private String endpoint(WildAiProperties.Payment.XunhuPay config, String path) {
        String gateway = requireText(config.getGateway(), "虎皮椒网关地址未配置");
        return gateway.replaceAll("/+$", "") + path;
    }

    private WildAiProperties.Payment.XunhuPay config() {
        return properties.getPayment().getXunhupay();
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, message);
        }
        return value;
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static String newNonce() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
