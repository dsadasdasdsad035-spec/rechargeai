package com.wildai.support.dto;

public record SupportWebSocketInboundMessage(
        String type,
        String sessionNo,
        String subject,
        String content) {
}
