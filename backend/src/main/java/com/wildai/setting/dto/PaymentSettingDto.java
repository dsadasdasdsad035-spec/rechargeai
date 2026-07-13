package com.wildai.setting.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentSettingDto(
        BigDecimal usdToCnyRate,
        Instant updatedAt
) {}
