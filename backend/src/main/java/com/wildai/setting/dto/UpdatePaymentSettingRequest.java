package com.wildai.setting.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record UpdatePaymentSettingRequest(
        @NotNull @DecimalMin("0.000001") BigDecimal usdToCnyRate
) {}
