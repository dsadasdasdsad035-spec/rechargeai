package com.wildai.finance.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateWithdrawalRequest(
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        @NotNull Long payoutAccountId,
        String remark
) {}
