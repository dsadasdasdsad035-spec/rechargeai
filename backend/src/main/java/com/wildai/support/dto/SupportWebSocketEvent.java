package com.wildai.support.dto;

public record SupportWebSocketEvent(
        String type,
        SupportSessionDto session,
        SupportMessageDto message) {
}
