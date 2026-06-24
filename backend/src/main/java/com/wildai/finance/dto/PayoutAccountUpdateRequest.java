package com.wildai.finance.dto;

public record PayoutAccountUpdateRequest(
        String accountName,
        String bankName,
        String accountNo,
        Boolean enabled
) {}
