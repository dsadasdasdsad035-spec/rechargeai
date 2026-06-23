package com.wildai.fulfillment.dto;

import jakarta.validation.constraints.NotBlank;

public record FulfillmentSupplementRequest(
        @NotBlank String content
) {}
