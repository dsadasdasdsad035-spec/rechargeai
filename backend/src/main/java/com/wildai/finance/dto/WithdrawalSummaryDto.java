package com.wildai.finance.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record WithdrawalSummaryDto(
        String withdrawalNo,
        BigDecimal amount,
        BigDecimal actualAmount,
        String status,
        Long payoutAccountId,
        String payoutAccountSummary,
        Long applicantAdminId,
        String applicantName,
        Instant appliedAt
) {}
