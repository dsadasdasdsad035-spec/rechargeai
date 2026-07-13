package com.wildai.support.dto;

import java.time.Instant;

public record SupportSessionDto(
        String sessionNo,
        Long userId,
        String subject,
        String status,
        String lastMessage,
        int unreadUserCount,
        int unreadAdminCount,
        Instant lastMessageAt,
        Instant createdAt,
        Instant updatedAt) {
}
