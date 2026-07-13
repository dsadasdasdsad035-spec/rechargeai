package com.wildai.support.service;

import com.wildai.notification.service.AdminNotificationMailService;
import com.wildai.setting.dto.NotificationSettingDto;
import com.wildai.setting.service.SystemSettingService;
import com.wildai.support.dto.SupportMessageDto;
import com.wildai.support.dto.SupportSessionDto;
import com.wildai.support.event.SupportMessageCreatedEvent;
import com.wildai.support.websocket.SupportOnlineStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SupportOfflineNotificationServiceTest {

    private final SupportOnlineStatus onlineStatus = mock(SupportOnlineStatus.class);
    private final SystemSettingService settingService = mock(SystemSettingService.class);
    private final AdminNotificationMailService mailService = mock(AdminNotificationMailService.class);
    private final SupportOfflineNotificationService service = new SupportOfflineNotificationService(
            onlineStatus,
            settingService,
            mailService
    );

    @Test
    void userMessageSendsMailWhenNoAdminOnline() {
        SupportSessionDto session = session();
        SupportMessageDto message = message("USER");
        when(onlineStatus.hasOnlineAdmin()).thenReturn(false);
        when(settingService.getNotificationSetting()).thenReturn(setting());

        service.onMessageCreated(new SupportMessageCreatedEvent(session, message));

        verify(mailService).sendSupportOfflineNotification("296629801@qq.com", session, message);
    }

    @Test
    void userMessageDoesNotSendMailWhenAdminOnline() {
        when(onlineStatus.hasOnlineAdmin()).thenReturn(true);

        service.onMessageCreated(new SupportMessageCreatedEvent(session(), message("USER")));

        verify(mailService, never()).sendSupportOfflineNotification(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void adminMessageDoesNotSendOfflineMail() {
        service.onMessageCreated(new SupportMessageCreatedEvent(session(), message("ADMIN")));

        verify(mailService, never()).sendSupportOfflineNotification(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    private NotificationSettingDto setting() {
        return new NotificationSettingDto(
                "296629801@qq.com",
                "296629801@qq.com",
                true,
                Instant.now()
        );
    }

    private SupportSessionDto session() {
        return new SupportSessionDto(
                "CS1001",
                7L,
                "订阅问题",
                "OPEN",
                "用户消息",
                0,
                1,
                Instant.now(),
                Instant.now(),
                Instant.now()
        );
    }

    private SupportMessageDto message(String senderType) {
        return new SupportMessageDto(
                12L,
                "CS1001",
                senderType,
                7L,
                "请帮我处理订阅",
                Instant.now()
        );
    }
}
