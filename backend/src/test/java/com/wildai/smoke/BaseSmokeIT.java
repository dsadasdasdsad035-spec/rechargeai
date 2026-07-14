package com.wildai.smoke;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wildai.order.repository.SubscriptionOrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.lifecycle.Startables;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.util.stream.Stream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 冒烟测试基础设施：Testcontainers + 通用 HTTP 辅助方法。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("smoke")
abstract class BaseSmokeIT {

    protected static final BigDecimal DEFAULT_SALE_PRICE = new BigDecimal("168.00");

    private static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    private static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
                    .withDatabaseName("wildai");

    static {
        // 冒烟测试在同一 Maven JVM 内共享容器，避免测试类切换后连接池仍指向已停止端口。
        Startables.deepStart(Stream.of(MYSQL, REDIS)).join();
    }

    @DynamicPropertySource
    static void containerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected SubscriptionOrderRepository orderRepo;

    protected record PaidOrder(String userToken, String orderNo, long productId, long orderId) {}

    protected record AdminSession(String username, String token, long adminId) {}

    protected String adminLogin() throws Exception {
        return adminLogin("admin", "changeme");
    }

    protected String adminLogin(String username, String password) throws Exception {
        MvcResult login = mockMvc.perform(post("/admin/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s"}
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andReturn();

        return objectMapper.readTree(login.getResponse().getContentAsString())
                .get("data").get("accessToken").asText();
    }

    protected AdminSession createSuperAdmin(String username, String password) throws Exception {
        String superToken = adminLogin();
        MvcResult created = mockMvc.perform(post("/admin/api/admins")
                        .header("Authorization", "Bearer " + superToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s","displayName":"审批员","roleCodes":["SUPER_ADMIN"]}
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andReturn();

        JsonNode data = objectMapper.readTree(created.getResponse().getContentAsString()).get("data");
        long adminId = data.get("id").asLong();
        String token = adminLogin(username, password);
        return new AdminSession(username, token, adminId);
    }

    protected long createAndShelfProduct(String adminToken, String name, BigDecimal salePrice) throws Exception {
        MvcResult create = mockMvc.perform(post("/admin/api/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","salePrice":%s,"periodDays":30,"currency":"CNY","serviceType":"GENERAL"}
                                """.formatted(name, salePrice.toPlainString())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
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

    protected PaidOrder createPaidOrder(String productNamePrefix, BigDecimal salePrice) throws Exception {
        String adminToken = adminLogin();
        long productId = createAndShelfProduct(adminToken, productNamePrefix + System.currentTimeMillis(), salePrice);

        String email = productNamePrefix + System.currentTimeMillis() + "@example.com";
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
                                {"productId":%d,"fields":{"target_account":"smoke@example.com","account_token":"test-token-smoke-12345678"}}
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

    protected record FinanceSnapshot(
            BigDecimal settledRevenue,
            BigDecimal availableBalance,
            BigDecimal frozenForWithdrawal,
            BigDecimal totalWithdrawn) {}

    protected FinanceSnapshot fetchFinanceOverview(String adminToken) throws Exception {
        MvcResult result = mockMvc.perform(get("/admin/api/finance/overview")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andReturn();

        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
        return new FinanceSnapshot(
                new BigDecimal(data.get("settledRevenue").asText()),
                new BigDecimal(data.get("availableBalance").asText()),
                new BigDecimal(data.get("frozenForWithdrawal").asText()),
                new BigDecimal(data.get("totalWithdrawn").asText()));
    }

    protected String sendEmailCode(String email) throws Exception {
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
