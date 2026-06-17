package com.wildai.order.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderDetailDto(
        String orderNo, Long productId, String productName, BigDecimal amount, String currency,
        String targetAccountMasked, String orderStatus, String paymentStatus, String fulfillmentStatus,
        String thirdTradeNoMasked, Instant createdAt, Instant paidAt, Instant expiredAt
) {}
