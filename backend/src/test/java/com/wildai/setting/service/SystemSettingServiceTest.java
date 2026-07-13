package com.wildai.setting.service;

import com.wildai.admin.service.AuditLogService;
import com.wildai.common.config.WildAiProperties;
import com.wildai.common.exception.BusinessException;
import com.wildai.setting.domain.SystemSetting;
import com.wildai.setting.dto.UpdateNotificationSettingRequest;
import com.wildai.setting.dto.UpdatePaymentSettingRequest;
import com.wildai.setting.repository.SystemSettingRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SystemSettingServiceTest {

    private final SystemSettingRepository settingRepo = mock(SystemSettingRepository.class);
    private final AuditLogService auditLogService = mock(AuditLogService.class);
    private final WildAiProperties properties = new WildAiProperties();
    private final SystemSettingService settingService = new SystemSettingService(settingRepo, properties, auditLogService);

    @Test
    void getUsdToCnyRateUsesDatabaseValue() {
        SystemSetting setting = setting("7.333333");
        when(settingRepo.findById(SystemSettingService.USD_TO_CNY_RATE)).thenReturn(Optional.of(setting));

        BigDecimal rate = settingService.getUsdToCnyRate();

        assertThat(rate).isEqualByComparingTo("7.333333");
    }

    @Test
    void getUsdToCnyRateFallsBackToPropertyWhenMissing() {
        properties.getPayment().setUsdToCnyRate(new BigDecimal("7.125"));
        when(settingRepo.findById(SystemSettingService.USD_TO_CNY_RATE)).thenReturn(Optional.empty());

        BigDecimal rate = settingService.getUsdToCnyRate();

        assertThat(rate).isEqualByComparingTo("7.125000");
    }

    @Test
    void updatePaymentSettingNormalizesAndAuditsRate() {
        SystemSetting existing = setting("7.250000");
        when(settingRepo.findById(SystemSettingService.USD_TO_CNY_RATE)).thenReturn(Optional.of(existing));
        when(settingRepo.save(any(SystemSetting.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var dto = settingService.updatePaymentSetting(9L, new UpdatePaymentSettingRequest(new BigDecimal("7.5")));

        assertThat(dto.usdToCnyRate()).isEqualByComparingTo("7.500000");
        assertThat(existing.getSettingValue()).isEqualTo("7.500000");
        assertThat(existing.getUpdatedBy()).isEqualTo(9L);
        verify(auditLogService).log(9L, "SYSTEM_SETTING_UPDATE", "SYSTEM_SETTING",
                SystemSettingService.USD_TO_CNY_RATE, "7.250000", "7.500000", "更新美元兑人民币支付汇率");
    }

    @Test
    void updatePaymentSettingRejectsZeroRate() {
        assertThatThrownBy(() -> settingService.updatePaymentSetting(9L,
                new UpdatePaymentSettingRequest(BigDecimal.ZERO)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("美元兑人民币汇率配置无效");

        verify(settingRepo, never()).save(any());
    }

    @Test
    void getNotificationSettingUsesDefaultAdminMailboxWhenMissing() {
        when(settingRepo.findById(SystemSettingService.SUPPORT_OFFLINE_NOTIFY_EMAIL)).thenReturn(Optional.empty());
        when(settingRepo.findById(SystemSettingService.ORDER_PAID_NOTIFY_EMAIL)).thenReturn(Optional.empty());
        when(settingRepo.findById(SystemSettingService.ORDER_PAID_NOTIFY_ENABLED)).thenReturn(Optional.empty());

        var dto = settingService.getNotificationSetting();

        assertThat(dto.supportOfflineNotifyEmail()).isEqualTo("296629801@qq.com");
        assertThat(dto.orderPaidNotifyEmail()).isEqualTo("296629801@qq.com");
        assertThat(dto.orderPaidNotifyEnabled()).isTrue();
    }

    @Test
    void updateNotificationSettingPersistsAndAuditsNotificationKeys() {
        SystemSetting supportEmail = setting(SystemSettingService.SUPPORT_OFFLINE_NOTIFY_EMAIL, "296629801@qq.com");
        SystemSetting orderEmail = setting(SystemSettingService.ORDER_PAID_NOTIFY_EMAIL, "296629801@qq.com");
        SystemSetting orderEnabled = setting(SystemSettingService.ORDER_PAID_NOTIFY_ENABLED, "true");
        when(settingRepo.findById(SystemSettingService.SUPPORT_OFFLINE_NOTIFY_EMAIL)).thenReturn(Optional.of(supportEmail));
        when(settingRepo.findById(SystemSettingService.ORDER_PAID_NOTIFY_EMAIL)).thenReturn(Optional.of(orderEmail));
        when(settingRepo.findById(SystemSettingService.ORDER_PAID_NOTIFY_ENABLED)).thenReturn(Optional.of(orderEnabled));
        when(settingRepo.save(any(SystemSetting.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var dto = settingService.updateNotificationSetting(9L, new UpdateNotificationSettingRequest(
                "support@example.com",
                "orders@example.com",
                false
        ));

        assertThat(dto.supportOfflineNotifyEmail()).isEqualTo("support@example.com");
        assertThat(dto.orderPaidNotifyEmail()).isEqualTo("orders@example.com");
        assertThat(dto.orderPaidNotifyEnabled()).isFalse();
        assertThat(supportEmail.getSettingValue()).isEqualTo("support@example.com");
        assertThat(orderEmail.getSettingValue()).isEqualTo("orders@example.com");
        assertThat(orderEnabled.getSettingValue()).isEqualTo("false");
        verify(auditLogService).log(eq(9L), eq("SYSTEM_SETTING_UPDATE"), eq("SYSTEM_SETTING"),
                eq("ADMIN_NOTIFICATION"), contains("296629801@qq.com"), contains("orders@example.com"),
                eq("更新管理员邮件通知设置"));
    }

    private SystemSetting setting(String value) {
        SystemSetting setting = new SystemSetting();
        setting.setSettingKey(SystemSettingService.USD_TO_CNY_RATE);
        setting.setSettingValue(value);
        setting.setDescription("美元兑人民币支付汇率");
        return setting;
    }

    private SystemSetting setting(String key, String value) {
        SystemSetting setting = new SystemSetting();
        setting.setSettingKey(key);
        setting.setSettingValue(value);
        setting.setDescription("测试配置");
        return setting;
    }
}
