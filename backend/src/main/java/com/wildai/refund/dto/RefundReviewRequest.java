package com.wildai.refund.dto;

import jakarta.validation.constraints.Size;

public record RefundReviewRequest(
        @Size(max = 512) String reviewComment
) {}
