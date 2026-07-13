package com.wildai.refund.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record AdminRefundDetailDto(
        String refundNo,
        String orderNo,
        Long userId,
        String productName,
        String paymentChannel,
        BigDecimal amount,
        String currency,
        String status,
        String applyReason,
        String reviewComment,
        Long reviewerAdminId,
        String reviewerName,
        String channelRefundNo,
        Instant createdAt,
        Instant updatedAt
) {}
