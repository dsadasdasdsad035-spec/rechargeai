package com.wildai.admin.web;

import com.wildai.admin.service.RbacService;
import com.wildai.common.dto.ApiResponse;
import com.wildai.common.security.UserPrincipal;
import com.wildai.setting.dto.NotificationSettingDto;
import com.wildai.setting.dto.PaymentSettingDto;
import com.wildai.setting.dto.UpdateNotificationSettingRequest;
import com.wildai.setting.dto.UpdatePaymentSettingRequest;
import com.wildai.setting.service.SystemSettingService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/api/settings")
public class AdminSettingController {

    private final SystemSettingService settingService;
    private final RbacService rbacService;

    public AdminSettingController(SystemSettingService settingService, RbacService rbacService) {
        this.settingService = settingService;
        this.rbacService = rbacService;
    }

    @GetMapping("/payment")
    public ApiResponse<PaymentSettingDto> payment(@AuthenticationPrincipal UserPrincipal principal) {
        rbacService.requireViewFinance(principal.id());
        return ApiResponse.ok(settingService.getPaymentSetting());
    }

    @PutMapping("/payment")
    public ApiResponse<PaymentSettingDto> updatePayment(@AuthenticationPrincipal UserPrincipal principal,
                                                        @Valid @RequestBody UpdatePaymentSettingRequest req) {
        rbacService.requireManageFinance(principal.id());
        return ApiResponse.ok(settingService.updatePaymentSetting(principal.id(), req));
    }

    @GetMapping("/notifications")
    public ApiResponse<NotificationSettingDto> notifications(@AuthenticationPrincipal UserPrincipal principal) {
        rbacService.requireManageAdmins(principal.id());
        return ApiResponse.ok(settingService.getNotificationSetting());
    }

    @PutMapping("/notifications")
    public ApiResponse<NotificationSettingDto> updateNotifications(@AuthenticationPrincipal UserPrincipal principal,
                                                                   @Valid @RequestBody UpdateNotificationSettingRequest req) {
        rbacService.requireManageAdmins(principal.id());
        return ApiResponse.ok(settingService.updateNotificationSetting(principal.id(), req));
    }
}
