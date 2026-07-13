package com.wildai.order.dto;

import com.wildai.fulfillment.dto.FulfillmentLogDto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record AdminOrderDetailDto(
        String orderNo, Long productId, String productName, BigDecimal amount, String currency,
        BigDecimal paidAmount, String paidCurrency, BigDecimal exchangeRate,
        String targetAccountMasked, String accountTokenMasked,
        String targetAccountPlain, String accountTokenPlain,
        String orderStatus, String paymentStatus, String fulfillmentStatus,
        String thirdTradeNoMasked, Instant createdAt, Instant paidAt, Instant expiredAt,
        String fulfillmentTaskNo, String fulfillmentTaskStatus,
        Instant subscriptionStart, Instant subscriptionEnd,
        List<FulfillmentLogDto> fulfillmentLogs,
        String refundNo, String refundStatus
) {}
