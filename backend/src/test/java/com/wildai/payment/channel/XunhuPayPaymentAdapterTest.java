package com.wildai.payment.channel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wildai.common.config.WildAiProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class XunhuPayPaymentAdapterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void createPaymentPostsSignedJsonAndMapsResponse() throws Exception {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        XunhuPayPaymentAdapter adapter = new XunhuPayPaymentAdapter(
                properties(),
                restTemplate,
                Clock.fixed(Instant.ofEpochSecond(1_700_000_000L), ZoneOffset.UTC),
                () -> "abc123abc123abc123abc123abc123ab"
        );
        Map<String, String> expectedBody = new LinkedHashMap<>();
        expectedBody.put("version", "1.1");
        expectedBody.put("appid", "app-001");
        expectedBody.put("trade_order_id", "P1001");
        expectedBody.put("total_fee", "12.30");
        expectedBody.put("title", "WildAI 订单 O1001");
        expectedBody.put("notify_url", "https://wildai.example/api/payments/xunhupay/notify");
        expectedBody.put("return_url", "https://wildai.example/pay/return");
        expectedBody.put("callback_url", "https://wildai.example/pay/cancel");
        expectedBody.put("time", "1700000000");
        expectedBody.put("nonce_str", "abc123abc123abc123abc123abc123ab");
        expectedBody.put("hash", XunhuPaySigner.sign(expectedBody, "secret-001"));

        server.expect(once(), requestTo("https://api.xunhupay.com/payment/do.html"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(content().json(objectMapper.writeValueAsString(expectedBody), true))
                .andRespond(withSuccess("""
                        {
                          "errcode": 0,
                          "errmsg": "success",
                          "data": {
                            "url": "https://pay.example/open",
                            "url_qrcode": "https://pay.example/qr.png",
                            "open_order_id": "HPJ1001"
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        Map<String, String> result = adapter.createPayment("P1001", new BigDecimal("12.3"), "O1001");

        assertThat(result).containsEntry("paymentNo", "P1001")
                .containsEntry("paymentUrl", "https://pay.example/open")
                .containsEntry("codeUrl", "https://pay.example/qr.png")
                .containsEntry("openOrderId", "HPJ1001");
        server.verify();
    }

    @Test
    void createPaymentSupportsFlatGatewayResponse() throws Exception {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        XunhuPayPaymentAdapter adapter = new XunhuPayPaymentAdapter(
                properties(),
                restTemplate,
                Clock.fixed(Instant.ofEpochSecond(1_700_000_000L), ZoneOffset.UTC),
                () -> "abc123abc123abc123abc123abc123ab"
        );

        server.expect(once(), requestTo("https://api.xunhupay.com/payment/do.html"))
                .andRespond(withSuccess("""
                        {
                          "openid": 20300739494,
                          "url_qrcode": "https://api.xunhupay.com/payments/wechat/qrcode",
                          "url": "https://api.xunhupay.com/payments/wechat/index",
                          "errcode": 0,
                          "errmsg": "success!"
                        }
                        """, MediaType.APPLICATION_JSON));

        Map<String, String> result = adapter.createPayment("P1002", new BigDecimal("1.00"), "O1002");

        assertThat(result).containsEntry("paymentUrl", "https://api.xunhupay.com/payments/wechat/index")
                .containsEntry("codeUrl", "https://api.xunhupay.com/payments/wechat/qrcode")
                .containsEntry("openOrderId", "20300739494");
        server.verify();
    }

    @Test
    void verifyCallbackUsesHashAndExtractsXunhuPayFields() {
        XunhuPayPaymentAdapter adapter = new XunhuPayPaymentAdapter(
                properties(),
                new RestTemplate(),
                Clock.systemUTC(),
                () -> "unused"
        );
        Map<String, String> params = new LinkedHashMap<>();
        params.put("trade_order_id", "P1001");
        params.put("total_fee", "12.30");
        params.put("transaction_id", "TX1001");
        params.put("open_order_id", "HPJ1001");
        params.put("status", "OD");
        params.put("appid", "app-001");
        params.put("time", "1700000000");
        params.put("nonce_str", "abc");
        params.put("hash", XunhuPaySigner.sign(params, "secret-001"));

        assertThat(adapter.verifyCallback(params, null)).isTrue();
        assertThat(adapter.extractPaymentNo(params)).isEqualTo("P1001");
        assertThat(adapter.extractThirdTradeNo(params)).isEqualTo("TX1001");
        assertThat(adapter.extractPaidAmount(params)).isEqualByComparingTo("12.30");
        assertThat(adapter.isPaidCallback(params)).isTrue();

        params.put("status", "WP");
        assertThat(adapter.isPaidCallback(params)).isFalse();

        params.put("hash", "bad");
        assertThat(adapter.verifyCallback(params, null)).isFalse();
    }

    private WildAiProperties properties() {
        WildAiProperties properties = new WildAiProperties();
        WildAiProperties.Payment.XunhuPay xunhuPay = properties.getPayment().getXunhupay();
        xunhuPay.setAppid("app-001");
        xunhuPay.setSecret("secret-001");
        xunhuPay.setGateway("https://api.xunhupay.com");
        xunhuPay.setNotifyUrl("https://wildai.example/api/payments/xunhupay/notify");
        xunhuPay.setReturnUrl("https://wildai.example/pay/return");
        xunhuPay.setCallbackUrl("https://wildai.example/pay/cancel");
        return properties;
    }
}
