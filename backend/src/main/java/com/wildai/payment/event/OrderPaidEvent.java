package com.wildai.payment.event;

import com.wildai.order.domain.SubscriptionOrder;
import com.wildai.payment.domain.PaymentTransaction;

public record OrderPaidEvent(
        SubscriptionOrder order,
        PaymentTransaction payment
) {}
