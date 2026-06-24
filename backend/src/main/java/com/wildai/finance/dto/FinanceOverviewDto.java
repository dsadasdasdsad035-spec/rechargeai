package com.wildai.finance.dto;

import java.math.BigDecimal;

public record FinanceOverviewDto(
        BigDecimal settledRevenue,
        BigDecimal totalRefunded,
        BigDecimal availableBalance,
        BigDecimal frozenForWithdrawal,
        BigDecimal totalWithdrawn
) {}
