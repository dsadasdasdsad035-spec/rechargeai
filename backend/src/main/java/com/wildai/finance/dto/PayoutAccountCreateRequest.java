package com.wildai.finance.dto;

import jakarta.validation.constraints.NotBlank;

public record PayoutAccountCreateRequest(
        @NotBlank String accountName,
        @NotBlank String bankName,
        @NotBlank String accountNo
) {}
