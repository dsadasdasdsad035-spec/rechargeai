package com.wildai.smoke;

import com.fasterxml.jackson.databind.JsonNode;
import com.wildai.finance.domain.LedgerEntryType;
import com.wildai.finance.domain.LedgerRefType;
import com.wildai.finance.repository.PlatformLedgerEntryRepository;
import com.wildai.fulfillment.repository.FulfillmentTaskRepository;
import com.wildai.order.domain.SubscriptionOrder;
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
 * 三期冒烟：履约 SUCCESS 结算 → 资金概览 → 收款账户 → 提现申请/审批/打款。
 */
class Phase3SmokeIT extends BaseSmokeIT {

    private static final BigDecimal WITHDRAW_AMOUNT = new BigDecimal("100.00");

    @Autowired
    FulfillmentTaskRepository fulfillmentTaskRepo;

    @Autowired
    PlatformLedgerEntryRepository ledgerEntryRepo;

    @Test
    @DisplayName("履约成功：订单 SUCCESS 且账本 SETTLE 入账")
    void fulfillmentSuccessSettlesLedgerSmoke() throws Exception {
        PaidOrder paid = createPaidOrder("p3-settle-", DEFAULT_SALE_PRICE);
        String adminToken = adminLogin();
        FinanceSnapshot before = fetchFinanceOverview(adminToken);
        String taskNo = fulfillmentTaskRepo.findByOrderId(paid.orderId()).orElseThrow().getTaskNo();

        mockMvc.perform(post("/admin/api/fulfillment-tasks/" + taskNo + "/success")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"subscriptionStart":"2026-01-01T00:00:00Z","subscriptionEnd":"2026-02-01T00:00:00Z","remark":"冒烟结算"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"));

        SubscriptionOrder order = orderRepo.findById(paid.orderId()).orElseThrow();
        assertThat(order.getOrderStatus()).isEqualTo("SUCCESS");
        assertThat(ledgerEntryRepo.existsByEntryTypeAndOrderId(LedgerEntryType.SETTLE, paid.orderId()))
                .isTrue();

        FinanceSnapshot after = fetchFinanceOverview(adminToken);
        assertThat(after.settledRevenue()).isEqualByComparingTo(before.settledRevenue().add(DEFAULT_SALE_PRICE));
        assertThat(after.availableBalance()).isEqualByComparingTo(before.availableBalance().add(DEFAULT_SALE_PRICE));
    }

    @Test
    @DisplayName("提现闭环：申请 → 审批 → 确认打款 → 资金概览与账本一致")
    void withdrawalFullCycleSmoke() throws Exception {
        String adminToken = adminLogin();
        markOrderSuccess(createPaidOrder("p3-withdraw-", DEFAULT_SALE_PRICE), adminToken);
        FinanceSnapshot beforeApply = fetchFinanceOverview(adminToken);

        long payoutAccountId = createPayoutAccount(adminToken);
        AdminSession approver = createSuperAdmin("approver-" + System.currentTimeMillis(), "changeme99");

        var apply = mockMvc.perform(post("/admin/api/withdrawals")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":100.00,"payoutAccountId":%d,"remark":"冒烟提现"}
                                """.formatted(payoutAccountId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.status").value("PENDING_APPROVAL"))
                .andReturn();

        String withdrawalNo = objectMapper.readTree(apply.getResponse().getContentAsString())
                .get("data").get("withdrawalNo").asText();

        FinanceSnapshot afterApply = fetchFinanceOverview(adminToken);
        assertThat(afterApply.availableBalance())
                .isEqualByComparingTo(beforeApply.availableBalance().subtract(WITHDRAW_AMOUNT));
        assertThat(afterApply.frozenForWithdrawal())
                .isEqualByComparingTo(beforeApply.frozenForWithdrawal().add(WITHDRAW_AMOUNT));

        mockMvc.perform(post("/admin/api/withdrawals/" + withdrawalNo + "/approve")
                        .header("Authorization", "Bearer " + approver.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

        mockMvc.perform(post("/admin/api/withdrawals/" + withdrawalNo + "/confirm-payout")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"actualAmount":100.00,"paidAt":"2026-06-24T10:00:00Z","externalVoucherNo":"SMOKE-V001"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        FinanceSnapshot afterPayout = fetchFinanceOverview(adminToken);
        assertThat(afterPayout.availableBalance()).isEqualByComparingTo(afterApply.availableBalance());
        assertThat(afterPayout.frozenForWithdrawal()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(afterPayout.totalWithdrawn())
                .isEqualByComparingTo(beforeApply.totalWithdrawn().add(WITHDRAW_AMOUNT));

        assertThat(ledgerEntryRepo.existsByEntryTypeAndRefTypeAndRefId(
                LedgerEntryType.WITHDRAW_COMPLETE, LedgerRefType.WITHDRAWAL, withdrawalNo)).isTrue();

        mockMvc.perform(get("/admin/api/withdrawals/export")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(org.hamcrest.Matchers.containsString(withdrawalNo)));
    }

    @Test
    @DisplayName("提现审批：不可审批自己发起的申请")
    void withdrawalSelfApproveRejectedSmoke() throws Exception {
        String adminToken = adminLogin();
        markOrderSuccess(createPaidOrder("p3-self-", DEFAULT_SALE_PRICE), adminToken);
        long payoutAccountId = createPayoutAccount(adminToken);

        var apply = mockMvc.perform(post("/admin/api/withdrawals")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":100.00,"payoutAccountId":%d}
                                """.formatted(payoutAccountId)))
                .andExpect(status().isOk())
                .andReturn();

        String withdrawalNo = objectMapper.readTree(apply.getResponse().getContentAsString())
                .get("data").get("withdrawalNo").asText();

        mockMvc.perform(post("/admin/api/withdrawals/" + withdrawalNo + "/approve")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("WITHDRAW_001"));
    }

    @Test
    @DisplayName("提现驳回：释放冻结并恢复可提现余额")
    void withdrawalRejectRestoresBalanceSmoke() throws Exception {
        String adminToken = adminLogin();
        markOrderSuccess(createPaidOrder("p3-reject-", DEFAULT_SALE_PRICE), adminToken);
        FinanceSnapshot beforeApply = fetchFinanceOverview(adminToken);
        long payoutAccountId = createPayoutAccount(adminToken);
        AdminSession approver = createSuperAdmin("rejector-" + System.currentTimeMillis(), "changeme99");

        var apply = mockMvc.perform(post("/admin/api/withdrawals")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":100.00,"payoutAccountId":%d}
                                """.formatted(payoutAccountId)))
                .andExpect(status().isOk())
                .andReturn();

        String withdrawalNo = objectMapper.readTree(apply.getResponse().getContentAsString())
                .get("data").get("withdrawalNo").asText();

        mockMvc.perform(post("/admin/api/withdrawals/" + withdrawalNo + "/reject")
                        .header("Authorization", "Bearer " + approver.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"资料不全\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"));

        FinanceSnapshot afterReject = fetchFinanceOverview(adminToken);
        assertThat(afterReject.availableBalance()).isEqualByComparingTo(beforeApply.availableBalance());
        assertThat(afterReject.frozenForWithdrawal()).isEqualByComparingTo(beforeApply.frozenForWithdrawal());

        assertThat(ledgerEntryRepo.existsByEntryTypeAndRefTypeAndRefId(
                LedgerEntryType.WITHDRAW_RELEASE, LedgerRefType.WITHDRAWAL, withdrawalNo)).isTrue();
    }

    private void markOrderSuccess(PaidOrder paid, String adminToken) throws Exception {
        String taskNo = fulfillmentTaskRepo.findByOrderId(paid.orderId()).orElseThrow().getTaskNo();
        mockMvc.perform(post("/admin/api/fulfillment-tasks/" + taskNo + "/success")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"subscriptionStart":"2026-01-01T00:00:00Z","subscriptionEnd":"2026-02-01T00:00:00Z"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"));
    }

    private long createPayoutAccount(String adminToken) throws Exception {
        var created = mockMvc.perform(post("/admin/api/payout-accounts")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"accountName":"冒烟收款户","bankName":"测试银行","accountNo":"6222021234567890123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.enabled").value(true))
                .andReturn();

        JsonNode data = objectMapper.readTree(created.getResponse().getContentAsString()).get("data");
        return data.get("id").asLong();
    }
}
