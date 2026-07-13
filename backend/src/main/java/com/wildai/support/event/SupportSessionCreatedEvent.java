package com.wildai.support.event;

import com.wildai.support.dto.SupportSessionDto;

public record SupportSessionCreatedEvent(SupportSessionDto session) {
}
