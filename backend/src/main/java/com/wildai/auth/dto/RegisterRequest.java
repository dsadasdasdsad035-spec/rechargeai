package com.wildai.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record RegisterRequest(
        @NotBlank String type,
        String phone,
        String email,
        String password,
        @NotBlank String verifyCode
) {}
