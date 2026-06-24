package com.wildai.smoke;

import com.wildai.fulfillment.domain.FulfillmentTask;
import com.wildai.fulfillment.repository.FulfillmentTaskRepository;
import com.wildai.notify.repository.OutboxEventRepository;
import com.wildai.order.domain.SubscriptionOrder;
import com.wildai.order.repository.SubscriptionOrderRepository;
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
 * 平台冒烟测试：一期支付闭环 + 二期履约/RBAC。
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

    @Autowired
    FulfillmentTaskRepository fulfillmentTaskRepo;

    @Autowired
    SubscriptionOrderRepository orderRepo;

    @Autowired
    OutboxEventRepository outboxEventRepo;

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
    @DisplayName("邮箱登录：注册 → 发码 → 登录")
    void emailLoginSmoke() throws Exception {
        String email = "login-" + System.currentTimeMillis() + "@example.com";
        String registerCode = sendEmailCode(email);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"EMAIL","email":"%s","verifyCode":"%s"}
                                """.formatted(email, registerCode)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"));

        String loginCode = sendEmailCode(email);
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","verifyCode":"%s"}
                                """.formatted(email, loginCode)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty());
    }

    @Test
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
    @DisplayName("Mock 支付：注册 → 下单 → 支付 → 回调 → 订单进入履约中")
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
                                {"productId":%d,"fields":{"target_account":"ai-user@example.com","account_token":"test-token-phase1-12345678"}}
                                """.formatted(productId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderNo").isNotEmpty())
                .andReturn();

        String orderNo = objectMapper.readTree(createOrder.getResponse().getContentAsString())
                .get("data").get("orderNo").asText();

        MvcResult pay = mockMvc.perform(post("/api/orders/" + orderNo + "/pay")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"channel\":\"XUNHUPAY\"}"))
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
                .andExpect(jsonPath("$.data.orderStatus").value("FULFILLING"))
                .andExpect(jsonPath("$.data.paymentStatus").value("PAID"));
    }

    @Test
    @DisplayName("管理端登录：响应包含 SUPER_ADMIN 角色")
    void adminLoginIncludesRolesSmoke() throws Exception {
        mockMvc.perform(post("/admin/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"changeme\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.roles").isArray())
                .andExpect(jsonPath("$.data.roles[?(@ == 'SUPER_ADMIN')]").exists());
    }

    @Test
    @DisplayName("支付成功：创建履约任务、写入 Outbox、订单详情可见日志")
    void fulfillmentCreatedAfterPaymentSmoke() throws Exception {
        PaidOrder paid = createPaidOrder();

        mockMvc.perform(get("/api/orders/" + paid.orderNo())
                        .header("Authorization", "Bearer " + paid.userToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.fulfillmentTaskNo").isNotEmpty())
                .andExpect(jsonPath("$.data.fulfillmentTaskStatus").value("PENDING"))
                .andExpect(jsonPath("$.data.fulfillmentStatus").value("PENDING"))
                .andExpect(jsonPath("$.data.fulfillmentLogs").isArray())
                .andExpect(jsonPath("$.data.fulfillmentLogs.length()").value(1))
                .andExpect(jsonPath("$.data.fulfillmentLogs[0].content").value(org.hamcrest.Matchers.containsString("履约任务已创建")));

        FulfillmentTask task = fulfillmentTaskRepo.findByOrderId(paid.orderId()).orElseThrow();
        assertThat(task.getStatus()).isEqualTo("PENDING");
        assertThat(outboxEventRepo.findAll().stream()
                .anyMatch(e -> "ORDER_FULFILLMENT_STARTED".equals(e.getEventType())))
                .isTrue();
    }

    @Test
    @DisplayName("履约中重复下单：同产品返回 409")
    void duplicateOrderWhileFulfillingSmoke() throws Exception {
        PaidOrder paid = createPaidOrder();

        mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + paid.userToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId":%d,"fields":{"target_account":"dup@example.com","account_token":"test-token-dup-12345678"}}
                                """.formatted(paid.productId())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ORDER_001"));
    }

    @Test
    @DisplayName("等待用户：补充资料后履约任务恢复处理中")
    void fulfillmentSupplementSmoke() throws Exception {
        PaidOrder paid = createPaidOrder();
        FulfillmentTask task = fulfillmentTaskRepo.findByOrderId(paid.orderId()).orElseThrow();
        task.setStatus("WAIT_USER");
        fulfillmentTaskRepo.save(task);

        SubscriptionOrder order = orderRepo.findById(paid.orderId()).orElseThrow();
        order.setFulfillmentStatus("WAIT_USER");
        orderRepo.save(order);

        mockMvc.perform(post("/api/orders/" + paid.orderNo() + "/fulfillment/supplement")
                        .header("Authorization", "Bearer " + paid.userToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"已在官网完成绑定\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.status").value("PROCESSING"));

        mockMvc.perform(get("/api/orders/" + paid.orderNo())
                        .header("Authorization", "Bearer " + paid.userToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fulfillmentTaskStatus").value("PROCESSING"))
                .andExpect(jsonPath("$.data.fulfillmentLogs.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(2)));
    }

    @Test
    @DisplayName("等待用户：自主确认后履约任务恢复处理中")
    void fulfillmentConfirmSmoke() throws Exception {
        PaidOrder paid = createPaidOrder();
        FulfillmentTask task = fulfillmentTaskRepo.findByOrderId(paid.orderId()).orElseThrow();
        task.setStatus("WAIT_USER");
        fulfillmentTaskRepo.save(task);

        mockMvc.perform(post("/api/orders/" + paid.orderNo() + "/fulfillment/confirm")
                        .header("Authorization", "Bearer " + paid.userToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PROCESSING"));

        assertThat(fulfillmentTaskRepo.findByOrderId(paid.orderId()).orElseThrow().getStatus())
                .isEqualTo("PROCESSING");
    }

    private record PaidOrder(String userToken, String orderNo, long productId, long orderId) {}

    private PaidOrder createPaidOrder() throws Exception {
        String adminToken = adminLogin();
        long productId = createAndShelfProduct(adminToken, "履约冒烟-" + System.currentTimeMillis());

        String email = "fulfill-" + System.currentTimeMillis() + "@example.com";
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
                                {"productId":%d,"fields":{"target_account":"fulfill@example.com","account_token":"test-token-fulfill-12345678"}}
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
