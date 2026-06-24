package com.wildai.finance.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;

public record ConfirmPayoutRequest(
        @NotNull @DecimalMin("0.01") BigDecimal actualAmount,
        @NotNull Instant paidAt,
        @NotBlank String externalVoucherNo,
        String varianceReason
) {}
