package com.wildai.product.dto;

public record ServiceTypeGuideDto(
        String serviceType,
        String displayName,
        String accountTutorial,
        String tokenTutorial
) {}
