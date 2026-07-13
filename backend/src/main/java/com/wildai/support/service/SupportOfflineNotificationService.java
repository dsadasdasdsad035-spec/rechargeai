package com.wildai.support.service;

import com.wildai.notification.service.AdminNotificationMailService;
import com.wildai.setting.service.SystemSettingService;
import com.wildai.support.event.SupportMessageCreatedEvent;
import com.wildai.support.websocket.SupportOnlineStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
public class SupportOfflineNotificationService {

    private final SupportOnlineStatus onlineStatus;
    private final SystemSettingService settingService;
    private final AdminNotificationMailService mailService;

    public SupportOfflineNotificationService(SupportOnlineStatus onlineStatus,
                                             SystemSettingService settingService,
                                             AdminNotificationMailService mailService) {
        this.onlineStatus = onlineStatus;
        this.settingService = settingService;
        this.mailService = mailService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onMessageCreated(SupportMessageCreatedEvent event) {
        if (!"USER".equals(event.message().senderType()) || onlineStatus.hasOnlineAdmin()) {
            return;
        }
        var setting = settingService.getNotificationSetting();
        mailService.sendSupportOfflineNotification(setting.supportOfflineNotifyEmail(), event.session(), event.message());
    }
}
