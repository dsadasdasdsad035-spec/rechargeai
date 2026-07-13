package com.wildai.notification.service;

import com.wildai.payment.event.OrderPaidEvent;
import com.wildai.setting.service.SystemSettingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
public class OrderPaidNotificationService {

    private final SystemSettingService settingService;
    private final AdminNotificationMailService mailService;

    public OrderPaidNotificationService(SystemSettingService settingService,
                                        AdminNotificationMailService mailService) {
        this.settingService = settingService;
        this.mailService = mailService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onOrderPaid(OrderPaidEvent event) {
        var setting = settingService.getNotificationSetting();
        if (!setting.orderPaidNotifyEnabled()) {
            return;
        }
        mailService.sendOrderPaidNotification(setting.orderPaidNotifyEmail(), event.order(), event.payment());
    }
}
