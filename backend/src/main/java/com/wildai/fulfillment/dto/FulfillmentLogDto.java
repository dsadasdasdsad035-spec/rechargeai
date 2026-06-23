package com.wildai.fulfillment.dto;

import java.time.Instant;

public record FulfillmentLogDto(
        String logType,
        String content,
        Instant createdAt
) {}
