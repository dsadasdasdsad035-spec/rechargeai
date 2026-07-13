package com.wildai.smoke;

import com.fasterxml.jackson.databind.JsonNode;
import com.wildai.finance.domain.LedgerEntryType;
import com.wildai.finance.repository.PlatformLedgerEntryRepository;
import com.wildai.fulfillment.repository.FulfillmentTaskRepository;
import com.wildai.order.domain.SubscriptionOrder;
import com.wildai.refund.repository.RefundRequestRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 后台展示口径：USD 订单按支付单锁定的人民币金额展示和入账。
 */
class AdminPaidAmountSmokeIT extends BaseSmokeIT {

    private static final BigDecimal USD_PRICE = new BigDecimal("25.99");
    private static final BigDecimal PAID_CNY_AMOUNT = new BigDecimal("188.43");

    @Autowired
    FulfillmentTaskRepository fulfillmentTaskRepo;

    @Autowired
    RefundRequestRepository refundRepo;

    @Autowired
    PlatformLedgerEntryRepository ledgerEntryRepo;

    @Test
    @DisplayName("USD 订单后台订单、履约、退款均展示锁定人民币实付金额")
    void usdOrderAdminViewsExposeLockedPaidCnyAmount() throws Exception {
        PaidOrder paid = createPaidUsdOrder("admin-paid-");
        String adminToken = adminLogin();

        mockMvc.perform(get("/admin/api/orders")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("orderNo", paid.orderNo()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].amount").value(25.99))
                .andExpect(jsonPath("$.data.items[0].currency").value("USD"))
                .andExpect(jsonPath("$.data.items[0].paidAmount").value(188.43))
                .andExpect(jsonPath("$.data.items[0].paidCurrency").value("CNY"))
                .andExpect(jsonPath("$.data.items[0].exchangeRate").value(7.25));

        mockMvc.perform(get("/admin/api/orders/" + paid.orderNo())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.amount").value(25.99))
                .andExpect(jsonPath("$.data.currency").value("USD"))
                .andExpect(jsonPath("$.data.paidAmount").value(188.43))
                .andExpect(jsonPath("$.data.paidCurrency").value("CNY"))
                .andExpect(jsonPath("$.data.exchangeRate").value(7.25));

        String taskNo = fulfillmentTaskRepo.findByOrderId(paid.orderId()).orElseThrow().getTaskNo();
        mockMvc.perform(get("/admin/api/fulfillment-tasks/" + taskNo)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.amount").value(25.99))
                .andExpect(jsonPath("$.data.currency").value("USD"))
                .andExpect(jsonPath("$.data.paidAmount").value(188.43))
                .andExpect(jsonPath("$.data.paidCurrency").value("CNY"));

        mockMvc.perform(post("/admin/api/fulfillment-tasks/" + taskNo + "/fail")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"failureReason\":\"第三方账号无法绑定\"}"))
                .andExpect(status().isOk());

        var apply = mockMvc.perform(post("/api/orders/" + paid.orderNo() + "/refunds")
                        .header("Authorization", "Bearer " + paid.userToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"applyReason\":\"履约失败，申请退款\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.amount").value(188.43))
                .andExpect(jsonPath("$.data.currency").value("CNY"))
                .andReturn();

        String refundNo = objectMapper.readTree(apply.getResponse().getContentAsString())
                .get("data").get("refundNo").asText();

        mockMvc.perform(get("/admin/api/refunds")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("refundNo", refundNo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].amount").value(188.43))
                .andExpect(jsonPath("$.data.items[0].currency").value("CNY"));

        mockMvc.perform(get("/admin/api/refunds/" + refundNo)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.amount").value(188.43))
                .andExpect(jsonPath("$.data.currency").value("CNY"));

        var refund = refundRepo.findByRefundNo(refundNo).orElseThrow();
        assertThat(refund.getAmount()).isEqualByComparingTo(PAID_CNY_AMOUNT);
    }

    @Test
    @DisplayName("USD 订单履约成功按锁定人民币实付金额结算入账")
    void usdOrderSuccessSettlesLockedPaidCnyAmount() throws Exception {
        PaidOrder paid = createPaidUsdOrder("admin-settle-");
        String adminToken = adminLogin();
        FinanceSnapshot before = fetchFinanceOverview(adminToken);
        String taskNo = fulfillmentTaskRepo.findByOrderId(paid.orderId()).orElseThrow().getTaskNo();

        mockMvc.perform(post("/admin/api/fulfillment-tasks/" + taskNo + "/success")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"subscriptionStart":"2026-01-01T00:00:00Z","subscriptionEnd":"2026-02-01T00:00:00Z","remark":"美元订单结算"}
                                """))
                .andExpect(status().isOk());

        FinanceSnapshot after = fetchFinanceOverview(adminToken);
        assertThat(after.settledRevenue()).isEqualByComparingTo(before.settledRevenue().add(PAID_CNY_AMOUNT));
        assertThat(after.availableBalance()).isEqualByComparingTo(before.availableBalance().add(PAID_CNY_AMOUNT));

        assertThat(ledgerEntryRepo.sumAmountByEntryType(LedgerEntryType.SETTLE))
                .isGreaterThanOrEqualTo(PAID_CNY_AMOUNT);
    }

    private PaidOrder createPaidUsdOrder(String productNamePrefix) throws Exception {
        String adminToken = adminLogin();
        long productId = createAndShelfUsdProduct(adminToken, productNamePrefix + System.currentTimeMillis());

        String email = productNamePrefix + System.currentTimeMillis() + "@example.com";
        String code = sendEmailCode(email);

        var register = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"EMAIL","email":"%s","verifyCode":"%s"}
                                """.formatted(email, code)))
                .andExpect(status().isOk())
                .andReturn();

        String token = objectMapper.readTree(register.getResponse().getContentAsString())
                .get("data").get("accessToken").asText();

        var createOrder = mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId":%d,"fields":{"target_account":"usd@example.com","account_token":"test-token-usd-12345678"}}
                                """.formatted(productId)))
                .andExpect(status().isOk())
                .andReturn();

        String orderNo = objectMapper.readTree(createOrder.getResponse().getContentAsString())
                .get("data").get("orderNo").asText();

        var pay = mockMvc.perform(post("/api/orders/" + orderNo + "/pay")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"channel\":\"XUNHUPAY\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.amount").value("188.43"))
                .andExpect(jsonPath("$.data.currency").value("CNY"))
                .andExpect(jsonPath("$.data.orderAmount").value("25.99"))
                .andExpect(jsonPath("$.data.orderCurrency").value("USD"))
                .andExpect(jsonPath("$.data.exchangeRate").value("7.250000"))
                .andReturn();

        JsonNode payData = objectMapper.readTree(pay.getResponse().getContentAsString()).get("data");
        mockMvc.perform(post("/api/payments/mock/notify")
                        .param("paymentNo", payData.get("paymentNo").asText())
                        .param("tradeNo", payData.get("mockTradeNo").asText()))
                .andExpect(status().isOk());

        SubscriptionOrder order = orderRepo.findByOrderNo(orderNo).orElseThrow();
        return new PaidOrder(token, orderNo, productId, order.getId());
    }

    private long createAndShelfUsdProduct(String adminToken, String name) throws Exception {
        var create = mockMvc.perform(post("/admin/api/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","salePrice":25.99,"periodDays":30,"currency":"USD","serviceType":"GENERAL"}
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
}
