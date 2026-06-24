package com.wildai.admin.dto;

import java.time.Instant;

public record AuditLogDto(
        Long id,
        Long operatorId,
        String operatorName,
        String operationType,
        String targetType,
        String targetId,
        String beforeValue,
        String afterValue,
        String reason,
        Instant createdAt
) {}
