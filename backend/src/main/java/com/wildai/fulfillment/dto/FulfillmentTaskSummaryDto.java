package com.wildai.fulfillment.dto;

import java.time.Instant;

public record FulfillmentTaskSummaryDto(
        String taskNo,
        String orderNo,
        String productName,
        String status,
        Long assigneeAdminId,
        String assigneeName,
        Instant createdAt,
        Instant updatedAt
) {}
