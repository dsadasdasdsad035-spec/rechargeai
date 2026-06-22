package com.wildai.smoke;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 一期冒烟测试：邮箱注册 + Mock 支付闭环。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("smoke")
@Testcontainers
class Phase1SmokeIT {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @Container
    @ServiceConnection
    static org.testcontainers.containers.MySQLContainer<?> mysql =
            new org.testcontainers.containers.MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
                    .withDatabaseName("wildai");

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    @DisplayName("邮箱注册：发码 → 注册 → 获取 token")
    void emailRegisterSmoke() throws Exception {
        String email = "smoke-" + System.currentTimeMillis() + "@example.com";
        String code = sendEmailCode(email);

        MvcResult register = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"EMAIL","email":"%s","verifyCode":"%s"}
                                """.formatted(email, code)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andReturn();

        JsonNode data = objectMapper.readTree(register.getResponse().getContentAsString()).get("data");
        assertThat(data.get("refreshToken").asText()).isNotBlank();
        assertThat(data.get("userNo").asText()).startsWith("U");
    }

    @Test
    @DisplayName("Mock 支付：注册 → 下单 → 支付 → 回调 → 订单已支付")
    void mockPaymentSmoke() throws Exception {
        String email = "pay-" + System.currentTimeMillis() + "@example.com";
        String code = sendEmailCode(email);

        MvcResult register = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"EMAIL","email":"%s","verifyCode":"%s"}
                                """.formatted(email, code)))
                .andExpect(status().isOk())
                .andReturn();

        String token = objectMapper.readTree(register.getResponse().getContentAsString())
                .get("data").get("accessToken").asText();

        MvcResult createOrder = mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId":1,"fields":{"target_account":"ai-user@example.com"}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderNo").isNotEmpty())
                .andReturn();

        String orderNo = objectMapper.readTree(createOrder.getResponse().getContentAsString())
                .get("data").get("orderNo").asText();

        MvcResult pay = mockMvc.perform(post("/api/orders/" + orderNo + "/pay")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"channel\":\"WECHAT\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.paymentNo").isNotEmpty())
                .andReturn();

        JsonNode payData = objectMapper.readTree(pay.getResponse().getContentAsString()).get("data");
        String paymentNo = payData.get("paymentNo").asText();
        String mockTradeNo = payData.get("mockTradeNo").asText();

        mockMvc.perform(post("/api/payments/mock/notify")
                        .param("paymentNo", paymentNo)
                        .param("tradeNo", mockTradeNo))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/orders/" + orderNo)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderStatus").value("PAID"))
                .andExpect(jsonPath("$.data.paymentStatus").value("PAID"));
    }

    private String sendEmailCode(String email) throws Exception {
        MvcResult send = mockMvc.perform(post("/api/auth/send-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.devCode").isNotEmpty())
                .andReturn();

        return objectMapper.readTree(send.getResponse().getContentAsString())
                .get("data").get("devCode").asText();
    }
}
