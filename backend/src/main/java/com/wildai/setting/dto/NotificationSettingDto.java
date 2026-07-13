package com.wildai.setting.dto;

import java.time.Instant;

public record NotificationSettingDto(
        String supportOfflineNotifyEmail,
        String orderPaidNotifyEmail,
        boolean orderPaidNotifyEnabled,
        Instant updatedAt
) {}
