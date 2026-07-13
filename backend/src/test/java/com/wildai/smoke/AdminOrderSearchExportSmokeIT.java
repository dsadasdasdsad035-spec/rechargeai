package com.wildai.smoke;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminOrderSearchExportSmokeIT extends BaseSmokeIT {

    @Test
    void adminOrdersSupportPaginationFilteringAndFilteredExport() throws Exception {
        PaidOrder first = createPaidOrder("admin-order-search-a-", new BigDecimal("88.00"));
        PaidOrder second = createPaidOrder("admin-order-search-b-", new BigDecimal("99.00"));
        String adminToken = adminLogin();

        mockMvc.perform(get("/admin/api/orders")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("paymentStatus", "PAID")
                        .param("pageNo", "2")
                        .param("pageSize", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.pageNo").value(2))
                .andExpect(jsonPath("$.data.pageSize").value(1))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.items", hasSize(1)));

        mockMvc.perform(get("/admin/api/orders")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("orderNo", first.orderNo()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].orderNo").value(first.orderNo()));

        String csv = mockMvc.perform(get("/admin/api/orders/export")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("orderNo", first.orderNo()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(first.orderNo())))
                .andExpect(content().string(not(containsString(second.orderNo()))))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(csv).startsWith("orderNo,product,orderAmount");
    }
}
