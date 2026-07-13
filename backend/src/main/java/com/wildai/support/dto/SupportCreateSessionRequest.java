package com.wildai.support.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SupportCreateSessionRequest(
        @NotBlank @Size(max = 128) String subject) {
}
