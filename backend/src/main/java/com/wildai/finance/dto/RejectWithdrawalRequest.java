package com.wildai.finance.dto;

import jakarta.validation.constraints.NotBlank;

public record RejectWithdrawalRequest(
        @NotBlank String reason
) {}
