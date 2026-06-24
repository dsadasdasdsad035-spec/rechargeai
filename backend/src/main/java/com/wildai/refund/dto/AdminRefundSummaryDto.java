package com.wildai.refund.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record AdminRefundSummaryDto(
        String refundNo,
        String orderNo,
        Long userId,
        BigDecimal amount,
        String status,
        String applyReason,
        Instant createdAt
) {}
