package com.wildai.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ForceOrderStatusRequest(
        String orderStatus,
        String paymentStatus,
        String fulfillmentStatus,
        @NotBlank @Size(max = 512) String reason
) {}
