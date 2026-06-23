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
 * 一期冒烟测试：邮箱注册 + 管理端新建产品 + Mock 支付闭环。
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
    @DisplayName("管理端：登录 → 新建产品 → 上架")
    void adminCreateProductSmoke() throws Exception {
        String adminToken = adminLogin();
        long productId = createAndShelfProduct(adminToken, "冒烟产品-" + System.currentTimeMillis());

        mockMvc.perform(get("/api/products/" + productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.status").value("ON_SHELF"))
                .andExpect(jsonPath("$.data.name").isNotEmpty());
    }

    @Test
    @DisplayName("Mock 支付：注册 → 新建产品 → 下单 → 支付 → 回调 → 订单已支付")
    void mockPaymentSmoke() throws Exception {
        String adminToken = adminLogin();
        long productId = createAndShelfProduct(adminToken, "支付冒烟-" + System.currentTimeMillis());

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
                                {"productId":%d,"fields":{"target_account":"ai-user@example.com"}}
                                """.formatted(productId)))
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

    private String adminLogin() throws Exception {
        MvcResult login = mockMvc.perform(post("/admin/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"changeme\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andReturn();

        return objectMapper.readTree(login.getResponse().getContentAsString())
                .get("data").get("accessToken").asText();
    }

    private long createAndShelfProduct(String adminToken, String name) throws Exception {
        MvcResult create = mockMvc.perform(post("/admin/api/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","salePrice":99.00,"periodDays":30,"currency":"CNY","serviceType":"GENERAL"}
                                """.formatted(name)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.id").isNotEmpty())
                .andExpect(jsonPath("$.data.productCode").isNotEmpty())
                .andReturn();

        long productId = objectMapper.readTree(create.getResponse().getContentAsString())
                .get("data").get("id").asLong();

        mockMvc.perform(post("/admin/api/products/" + productId + "/shelf")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ON_SHELF\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"));

        return productId;
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
