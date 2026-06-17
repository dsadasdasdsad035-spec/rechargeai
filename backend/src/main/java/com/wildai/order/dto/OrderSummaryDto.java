package com.wildai.order.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderSummaryDto(
        String orderNo, String productName, BigDecimal amount, String currency,
        String orderStatus, String paymentStatus, String fulfillmentStatus,
        Instant createdAt, Instant paidAt
) {}
