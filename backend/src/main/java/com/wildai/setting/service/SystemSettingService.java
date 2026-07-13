package com.wildai.setting.service;

import com.wildai.admin.service.AuditLogService;
import com.wildai.common.config.WildAiProperties;
import com.wildai.common.exception.BusinessException;
import com.wildai.common.exception.ErrorCode;
import com.wildai.setting.domain.SystemSetting;
import com.wildai.setting.dto.NotificationSettingDto;
import com.wildai.setting.dto.PaymentSettingDto;
import com.wildai.setting.dto.UpdateNotificationSettingRequest;
import com.wildai.setting.dto.UpdatePaymentSettingRequest;
import com.wildai.setting.repository.SystemSettingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Objects;
import java.util.stream.Stream;

@Service
public class SystemSettingService {

    public static final String USD_TO_CNY_RATE = "USD_TO_CNY_RATE";
    public static final String SUPPORT_OFFLINE_NOTIFY_EMAIL = "SUPPORT_OFFLINE_NOTIFY_EMAIL";
    public static final String ORDER_PAID_NOTIFY_EMAIL = "ORDER_PAID_NOTIFY_EMAIL";
    public static final String ORDER_PAID_NOTIFY_ENABLED = "ORDER_PAID_NOTIFY_ENABLED";
    private static final String DEFAULT_ADMIN_NOTIFY_EMAIL = "296629801@qq.com";
    private static final int RATE_SCALE = 6;

    private final SystemSettingRepository settingRepo;
    private final WildAiProperties properties;
    private final AuditLogService auditLogService;

    public SystemSettingService(SystemSettingRepository settingRepo,
                                WildAiProperties properties,
                                AuditLogService auditLogService) {
        this.settingRepo = settingRepo;
        this.properties = properties;
        this.auditLogService = auditLogService;
    }

    public PaymentSettingDto getPaymentSetting() {
        SystemSetting setting = settingRepo.findById(USD_TO_CNY_RATE).orElse(null);
        return new PaymentSettingDto(getUsdToCnyRate(), setting != null ? setting.getUpdatedAt() : null);
    }

    public BigDecimal getUsdToCnyRate() {
        return settingRepo.findById(USD_TO_CNY_RATE)
                .map(SystemSetting::getSettingValue)
                .map(this::parseRate)
                .orElseGet(this::fallbackUsdToCnyRate);
    }

    public NotificationSettingDto getNotificationSetting() {
        SystemSetting supportEmail = settingRepo.findById(SUPPORT_OFFLINE_NOTIFY_EMAIL).orElse(null);
        SystemSetting orderEmail = settingRepo.findById(ORDER_PAID_NOTIFY_EMAIL).orElse(null);
        SystemSetting orderEnabled = settingRepo.findById(ORDER_PAID_NOTIFY_ENABLED).orElse(null);
        return new NotificationSettingDto(
                nonBlankOrDefault(supportEmail != null ? supportEmail.getSettingValue() : null, DEFAULT_ADMIN_NOTIFY_EMAIL),
                nonBlankOrDefault(orderEmail != null ? orderEmail.getSettingValue() : null, DEFAULT_ADMIN_NOTIFY_EMAIL),
                parseBooleanSetting(orderEnabled != null ? orderEnabled.getSettingValue() : null, true),
                latestUpdatedAt(supportEmail, orderEmail, orderEnabled)
        );
    }

    @Transactional
    public PaymentSettingDto updatePaymentSetting(Long adminId, UpdatePaymentSettingRequest req) {
        BigDecimal rate = normalizeRate(req.usdToCnyRate());
        SystemSetting setting = settingRepo.findById(USD_TO_CNY_RATE).orElseGet(() -> {
            SystemSetting created = new SystemSetting();
            created.setSettingKey(USD_TO_CNY_RATE);
            created.setDescription("美元兑人民币支付汇率");
            created.setCreatedAt(Instant.now());
            return created;
        });
        String before = setting.getSettingValue();
        String after = rate.toPlainString();
        setting.setSettingValue(after);
        setting.setDescription("美元兑人民币支付汇率");
        setting.setUpdatedBy(adminId);
        setting.setUpdatedAt(Instant.now());
        SystemSetting saved = settingRepo.save(setting);

        auditLogService.log(adminId, "SYSTEM_SETTING_UPDATE", "SYSTEM_SETTING", USD_TO_CNY_RATE,
                before, after, "更新美元兑人民币支付汇率");
        return new PaymentSettingDto(rate, saved.getUpdatedAt());
    }

    @Transactional
    public NotificationSettingDto updateNotificationSetting(Long adminId, UpdateNotificationSettingRequest req) {
        String supportEmail = normalizeEmail(req.supportOfflineNotifyEmail(), "离线客服提醒邮箱配置无效");
        String orderEmail = normalizeEmail(req.orderPaidNotifyEmail(), "订单支付完成提醒邮箱配置无效");
        if (req.orderPaidNotifyEnabled() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "订单支付完成邮件通知开关配置无效");
        }
        NotificationSettingDto beforeDto = getNotificationSetting();
        Instant now = Instant.now();
        SystemSetting savedSupportEmail = upsertSetting(SUPPORT_OFFLINE_NOTIFY_EMAIL, supportEmail,
                "离线客服邮件提醒邮箱", adminId, now);
        SystemSetting savedOrderEmail = upsertSetting(ORDER_PAID_NOTIFY_EMAIL, orderEmail,
                "订单支付完成邮件提醒邮箱", adminId, now);
        SystemSetting savedOrderEnabled = upsertSetting(ORDER_PAID_NOTIFY_ENABLED,
                req.orderPaidNotifyEnabled().toString(), "订单支付完成邮件提醒开关", adminId, now);
        NotificationSettingDto afterDto = new NotificationSettingDto(
                savedSupportEmail.getSettingValue(),
                savedOrderEmail.getSettingValue(),
                parseBooleanSetting(savedOrderEnabled.getSettingValue(), true),
                latestUpdatedAt(savedSupportEmail, savedOrderEmail, savedOrderEnabled)
        );

        auditLogService.log(adminId, "SYSTEM_SETTING_UPDATE", "SYSTEM_SETTING", "ADMIN_NOTIFICATION",
                notificationSummary(beforeDto), notificationSummary(afterDto), "更新管理员邮件通知设置");
        return afterDto;
    }

    private BigDecimal parseRate(String value) {
        try {
            return normalizeRate(new BigDecimal(value));
        } catch (RuntimeException e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "美元兑人民币汇率配置无效");
        }
    }

    private BigDecimal fallbackUsdToCnyRate() {
        return normalizeRate(properties.getPayment().getUsdToCnyRate());
    }

    private BigDecimal normalizeRate(BigDecimal rate) {
        if (rate == null || rate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "美元兑人民币汇率配置无效");
        }
        return rate.setScale(RATE_SCALE, RoundingMode.HALF_UP);
    }

    private SystemSetting upsertSetting(String key, String value, String description, Long adminId, Instant now) {
        SystemSetting setting = settingRepo.findById(key).orElseGet(() -> {
            SystemSetting created = new SystemSetting();
            created.setSettingKey(key);
            created.setCreatedAt(now);
            return created;
        });
        setting.setSettingValue(value);
        setting.setDescription(description);
        setting.setUpdatedBy(adminId);
        setting.setUpdatedAt(now);
        return settingRepo.save(setting);
    }

    private String normalizeEmail(String email, String errorMessage) {
        if (email == null || email.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, errorMessage);
        }
        return email.trim();
    }

    private String nonBlankOrDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    private boolean parseBooleanSetting(String value, boolean defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        if ("true".equalsIgnoreCase(value.trim())) {
            return true;
        }
        if ("false".equalsIgnoreCase(value.trim())) {
            return false;
        }
        throw new BusinessException(ErrorCode.BAD_REQUEST, "订单支付完成邮件通知开关配置无效");
    }

    private Instant latestUpdatedAt(SystemSetting... settings) {
        return Stream.of(settings)
                .filter(Objects::nonNull)
                .map(SystemSetting::getUpdatedAt)
                .filter(Objects::nonNull)
                .max(Instant::compareTo)
                .orElse(null);
    }

    private String notificationSummary(NotificationSettingDto dto) {
        return "supportOfflineNotifyEmail=%s, orderPaidNotifyEmail=%s, orderPaidNotifyEnabled=%s"
                .formatted(dto.supportOfflineNotifyEmail(), dto.orderPaidNotifyEmail(), dto.orderPaidNotifyEnabled());
    }
}
