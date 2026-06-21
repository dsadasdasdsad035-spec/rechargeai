package com.wildai.payment.channel;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class XunhuPaySignerTest {

    @Test
    void signFiltersEmptyValuesAndSortsByAsciiName() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("total_fee", "9.90");
        params.put("hash", "should-be-ignored");
        params.put("empty", "");
        params.put("appid", "test123");
        params.put("nonce_str", "abc");
        params.put("time", "1700000000");

        String sign = XunhuPaySigner.sign(params, "mysecret");

        assertThat(sign).isEqualTo("53ed2802e729ad481445ec83ac158400");
    }

    @Test
    void verifyRejectsTamperedHash() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("appid", "test123");
        params.put("trade_order_id", "P1001");
        params.put("total_fee", "9.90");
        params.put("hash", XunhuPaySigner.sign(params, "mysecret"));

        assertThat(XunhuPaySigner.verify(params, "mysecret")).isTrue();

        params.put("total_fee", "19.90");
        assertThat(XunhuPaySigner.verify(params, "mysecret")).isFalse();
    }
}
