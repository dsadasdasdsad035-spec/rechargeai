package com.wildai.fulfillment.dto;

import java.time.Instant;

public record AdminFulfillmentLogDto(
        String logType,
        String content,
        boolean userVisible,
        Long operatorId,
        String operatorName,
        Instant createdAt
) {}
