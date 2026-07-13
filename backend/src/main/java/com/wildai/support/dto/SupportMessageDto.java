package com.wildai.support.dto;

import java.time.Instant;

public record SupportMessageDto(
        Long id,
        String sessionNo,
        String senderType,
        Long senderId,
        String content,
        Instant createdAt) {
}
