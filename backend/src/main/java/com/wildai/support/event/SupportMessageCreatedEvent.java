package com.wildai.support.event;

import com.wildai.support.dto.SupportMessageDto;
import com.wildai.support.dto.SupportSessionDto;

public record SupportMessageCreatedEvent(
        SupportSessionDto session,
        SupportMessageDto message) {
}
