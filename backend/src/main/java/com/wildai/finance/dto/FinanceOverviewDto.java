package com.wildai.finance.dto;

import java.math.BigDecimal;

public record FinanceOverviewDto(
        BigDecimal settledRevenue,
        /** 退款单 COMPLETED 累计，渠道原路退回用户 */
        BigDecimal userRefunded,
        BigDecimal availableBalance,
        BigDecimal frozenForWithdrawal,
        BigDecimal totalWithdrawn
) {}
