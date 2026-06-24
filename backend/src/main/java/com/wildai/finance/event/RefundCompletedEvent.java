package com.wildai.finance.event;

import java.math.BigDecimal;

public record RefundCompletedEvent(String refundNo, Long orderId, BigDecimal amount) {}
