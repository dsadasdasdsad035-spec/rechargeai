package com.wildai.refund.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record RefundRequestDto(
        String refundNo,
        String orderNo,
        BigDecimal amount,
        String currency,
        String status,
        String applyReason,
        String reviewComment,
        Instant createdAt,
        Instant updatedAt
) {}
