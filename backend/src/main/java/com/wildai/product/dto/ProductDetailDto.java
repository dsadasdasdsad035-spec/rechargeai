package com.wildai.product.dto;

import java.math.BigDecimal;

public record ProductDetailDto(
        Long id, String productCode, String name, String serviceType,
        BigDecimal officialPrice, BigDecimal salePrice, String currency,
        Integer periodDays, String status, String requiredFieldsJson,
        Integer estimatedHours, String refundPolicyText, String complianceNotice,
        ServiceTypeGuideDto serviceTypeGuide
) {}
