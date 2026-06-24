package com.wildai.refund.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RefundApplyRequest(
        @NotBlank @Size(max = 512) String applyReason
) {}
