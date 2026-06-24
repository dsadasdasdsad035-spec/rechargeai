package com.wildai.finance.dto;

public record PayoutAccountSummaryDto(
        Long id,
        String accountName,
        String bankName,
        String accountLast4,
        boolean enabled
) {}
