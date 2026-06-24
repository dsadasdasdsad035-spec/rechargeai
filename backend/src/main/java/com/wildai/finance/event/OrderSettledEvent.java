package com.wildai.finance.event;

import java.math.BigDecimal;

public record OrderSettledEvent(String orderNo, Long orderId, BigDecimal amount) {}
