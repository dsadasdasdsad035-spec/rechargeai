package com.wildai.setting.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateNotificationSettingRequest(
        @NotBlank @Email @Size(max = 128) String supportOfflineNotifyEmail,
        @NotBlank @Email @Size(max = 128) String orderPaidNotifyEmail,
        @NotNull Boolean orderPaidNotifyEnabled
) {}
