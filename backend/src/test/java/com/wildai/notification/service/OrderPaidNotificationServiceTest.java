package com.wildai.notification.service;

import com.wildai.order.domain.SubscriptionOrder;
import com.wildai.payment.domain.PaymentTransaction;
import com.wildai.payment.event.OrderPaidEvent;
import com.wildai.setting.dto.NotificationSettingDto;
import com.wildai.setting.service.SystemSettingService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderPaidNotificationServiceTest {

    private final SystemSettingService settingService = mock(SystemSettingService.class);
    private final AdminNotificationMailService mailService = mock(AdminNotificationMailService.class);
    private final OrderPaidNotificationService service = new OrderPaidNotificationService(settingService, mailService);

    @Test
    void sendsMailWhenOrderPaidNotificationEnabled() {
        SubscriptionOrder order = order();
        PaymentTransaction payment = payment();
        when(settingService.getNotificationSetting()).thenReturn(setting(true));

        service.onOrderPaid(new OrderPaidEvent(order, payment));

        verify(mailService).sendOrderPaidNotification("orders@example.com", order, payment);
    }

    @Test
    void skipsMailWhenOrderPaidNotificationDisabled() {
        when(settingService.getNotificationSetting()).thenReturn(setting(false));

        service.onOrderPaid(new OrderPaidEvent(order(), payment()));

        verify(mailService, never()).sendOrderPaidNotification(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    private NotificationSettingDto setting(boolean enabled) {
        return new NotificationSettingDto(
                "support@example.com",
                "orders@example.com",
                enabled,
                Instant.now()
        );
    }

    private SubscriptionOrder order() {
        SubscriptionOrder order = new SubscriptionOrder();
        order.setId(42L);
        order.setOrderNo("O1001");
        order.setUserId(7L);
        order.setProductId(3L);
        order.setAmount(new BigDecimal("25.99"));
        order.setCurrency("USD");
        order.setOrderStatus("FULFILLING");
        order.setPaymentStatus("PAID");
        order.setPaidAt(Instant.now());
        return order;
    }

    private PaymentTransaction payment() {
        PaymentTransaction payment = new PaymentTransaction();
        payment.setPaymentNo("P1001");
        payment.setOrderId(42L);
        payment.setChannel("XUNHUPAY");
        payment.setAmount(new BigDecimal("188.43"));
        payment.setCurrency("CNY");
        payment.setOrderAmount(new BigDecimal("25.99"));
        payment.setOrderCurrency("USD");
        payment.setExchangeRate(new BigDecimal("7.250000"));
        payment.setStatus("PAID");
        payment.setThirdTradeNo("TX1001");
        payment.setPaidAt(Instant.now());
        return payment;
    }
}
