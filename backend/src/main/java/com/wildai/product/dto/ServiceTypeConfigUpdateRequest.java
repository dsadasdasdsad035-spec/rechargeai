package com.wildai.product.dto;

public record ServiceTypeConfigUpdateRequest(
        String displayName,
        String accountTutorial,
        String tokenTutorial
) {}
