package com.wildai.payment.channel;

import com.wildai.common.config.WildAiProperties;
import com.wildai.common.exception.BusinessException;
import com.wildai.common.exception.ErrorCode;
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
        params.put("title", "WildAI 订单 " + orderNo);
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
        return Map.of(
                "paymentNo", paymentNo,
                "paymentUrl", stringValue(data.get("url")),
                "codeUrl", stringValue(data.get("url_qrcode")),
                "openOrderId", stringValue(data.get("open_order_id"))
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
        return transactionId != null ? transactionId : params.get("open_order_id");
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

    @SuppressWarnings("unchecked")
    private Map<String, Object> postCreatePayment(WildAiProperties.Payment.XunhuPay config, Map<String, String> params) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, Object> response = restTemplate.postForObject(endpoint(config, "/payment/do.html"), new HttpEntity<>(params, headers), Map.class);
            if (response == null) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "虎皮椒支付网关返回为空");
            }
            return response;
        } catch (BusinessException e) {
            throw e;
        } catch (RestClientException e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "虎皮椒支付网关请求失败");
        }
    }

    private Map<?, ?> responseData(Map<String, Object> response) {
        Object data = response.get("data");
        if (data instanceof Map<?, ?> map) {
            return map;
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
