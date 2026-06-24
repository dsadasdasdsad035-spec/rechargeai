package com.wildai.finance.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record WithdrawalDetailDto(
        String withdrawalNo,
        BigDecimal amount,
        BigDecimal actualAmount,
        String status,
        Long payoutAccountId,
        String payoutAccountSummary,
        Long applicantAdminId,
        String applicantName,
        Long approverAdminId,
        String approverName,
        Long payoutConfirmerId,
        String payoutConfirmerName,
        String externalVoucherNo,
        String remark,
        String rejectReason,
        String varianceReason,
        Instant appliedAt,
        Instant approvedAt,
        Instant paidAt
) {}
