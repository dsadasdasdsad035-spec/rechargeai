package com.wildai.finance.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record LedgerEntryDto(
        String entryNo,
        String entryType,
        BigDecimal amount,
        BigDecimal balanceAfter,
        String refType,
        String refId,
        Long orderId,
        Instant createdAt
) {}
