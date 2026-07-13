package com.wildai.support.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SupportSendMessageRequest(
        @NotBlank @Size(max = 1000) String content) {
}
