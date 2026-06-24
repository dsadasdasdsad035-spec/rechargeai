package com.wildai.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wildai.fulfillment.repository.FulfillmentTaskRepository;
import com.wildai.order.domain.SubscriptionOrder;
import com.wildai.order.repository.SubscriptionOrderRepository;
import com.wildai.refund.repository.RefundRequestRepository;
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

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 二期集成：支付 → 履约失败 → 用户退款 → 审核退款完成。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("smoke")
@Testcontainers
class Phase2FlowIT {

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

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired FulfillmentTaskRepository fulfillmentTaskRepo;
    @Autowired SubscriptionOrderRepository orderRepo;
    @Autowired RefundRequestRepository refundRepo;

    @Test
    @DisplayName("履约失败 → 退款申请 → 审核通过 → 订单已退款")
    void fulfillmentFailedThenRefundCompleted() throws Exception {
        PaidOrder paid = createPaidOrder();
        String adminToken = adminLogin();
        String taskNo = fulfillmentTaskRepo.findByOrderId(paid.orderId()).orElseThrow().getTaskNo();

        mockMvc.perform(post("/admin/api/fulfillment-tasks/" + taskNo + "/fail")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"failureReason\":\"第三方账号无法绑定\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"));

        SubscriptionOrder order = orderRepo.findById(paid.orderId()).orElseThrow();
        assertThat(order.getOrderStatus()).isEqualTo("FAILED");
        assertThat(order.getFulfillmentStatus()).isEqualTo("FAILED");

        MvcResult apply = mockMvc.perform(post("/api/orders/" + paid.orderNo() + "/refunds")
                        .header("Authorization", "Bearer " + paid.userToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"applyReason\":\"履约失败，申请全额退款\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andReturn();

        String refundNo = objectMapper.readTree(apply.getResponse().getContentAsString())
                .get("data").get("refundNo").asText();

        mockMvc.perform(post("/admin/api/refunds/" + refundNo + "/approve")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reviewComment\":\"同意退款\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        order = orderRepo.findById(paid.orderId()).orElseThrow();
        assertThat(order.getOrderStatus()).isEqualTo("REFUNDED");
        assertThat(order.getPaymentStatus()).isEqualTo("REFUNDED");

        var refund = refundRepo.findByRefundNo(refundNo).orElseThrow();
        assertThat(refund.getStatus()).isEqualTo("COMPLETED");
        assertThat(refund.getAmount()).isEqualByComparingTo(new BigDecimal("99.00"));
    }

    private record PaidOrder(String userToken, String orderNo, long productId, long orderId) {}

    private PaidOrder createPaidOrder() throws Exception {
        String adminToken = adminLogin();
        long productId = createAndShelfProduct(adminToken, "二期退款流-" + System.currentTimeMillis());

        String email = "p2-" + System.currentTimeMillis() + "@example.com";
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
                                {"productId":%d,"fields":{"target_account":"refund-flow@example.com","account_token":"test-token-refund-12345678"}}
                                """.formatted(productId)))
                .andExpect(status().isOk())
                .andReturn();

        String orderNo = objectMapper.readTree(createOrder.getResponse().getContentAsString())
                .get("data").get("orderNo").asText();

        MvcResult pay = mockMvc.perform(post("/api/orders/" + orderNo + "/pay")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"channel\":\"XUNHUPAY\"}"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode payData = objectMapper.readTree(pay.getResponse().getContentAsString()).get("data");
        mockMvc.perform(post("/api/payments/mock/notify")
                        .param("paymentNo", payData.get("paymentNo").asText())
                        .param("tradeNo", payData.get("mockTradeNo").asText()))
                .andExpect(status().isOk());

        long orderId = orderRepo.findByOrderNo(orderNo).orElseThrow().getId();
        return new PaidOrder(token, orderNo, productId, orderId);
    }

    private String adminLogin() throws Exception {
        MvcResult login = mockMvc.perform(post("/admin/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"changeme\"}"))
                .andExpect(status().isOk())
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
                .andReturn();

        long productId = objectMapper.readTree(create.getResponse().getContentAsString())
                .get("data").get("id").asLong();

        mockMvc.perform(post("/admin/api/products/" + productId + "/shelf")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ON_SHELF\"}"))
                .andExpect(status().isOk());

        return productId;
    }

    private String sendEmailCode(String email) throws Exception {
        MvcResult send = mockMvc.perform(post("/api/auth/send-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(send.getResponse().getContentAsString())
                .get("data").get("devCode").asText();
    }
}
