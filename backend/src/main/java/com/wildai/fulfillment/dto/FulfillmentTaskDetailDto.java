package com.wildai.fulfillment.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record FulfillmentTaskDetailDto(
        String taskNo,
        String orderNo,
        Long userId,
        String productName,
        BigDecimal amount,
        String currency,
        BigDecimal paidAmount,
        String paidCurrency,
        BigDecimal exchangeRate,
        String orderStatus,
        String paymentStatus,
        String fulfillmentStatus,
        String status,
        Long assigneeAdminId,
        String assigneeName,
        Instant subscriptionStart,
        Instant subscriptionEnd,
        String failureReason,
        Instant createdAt,
        Instant updatedAt,
        List<AdminFulfillmentLogDto> logs
) {}
