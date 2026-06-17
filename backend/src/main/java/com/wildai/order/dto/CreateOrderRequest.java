package com.wildai.order.dto;

import jakarta.validation.constraints.NotNull;
import java.util.Map;

public record CreateOrderRequest(@NotNull Long productId, Map<String, String> fields) {}
