package com.wildai.admin.web;

import com.wildai.admin.dto.*;
import com.wildai.admin.service.AdminRoleManagementService;
import com.wildai.common.dto.ApiResponse;
import com.wildai.common.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/api")
public class AdminRoleController {

    private final AdminRoleManagementService roleManagementService;

    public AdminRoleController(AdminRoleManagementService roleManagementService) {
        this.roleManagementService = roleManagementService;
    }

    @GetMapping("/roles")
    public ApiResponse<List<AdminRoleDto>> listRoles() {
        return ApiResponse.ok(roleManagementService.listRoles());
    }

    @GetMapping("/admins")
    public ApiResponse<List<AdminUserDto>> listAdmins() {
        return ApiResponse.ok(roleManagementService.listAdmins());
    }

    @PostMapping("/admins")
    public ApiResponse<AdminUserDto> createAdmin(@AuthenticationPrincipal UserPrincipal principal,
                                                 @Valid @RequestBody CreateAdminRequest req) {
        return ApiResponse.ok(roleManagementService.createAdmin(principal.id(), req));
    }

    @PutMapping("/admins/{id}/roles")
    public ApiResponse<AdminUserDto> setRoles(@AuthenticationPrincipal UserPrincipal principal,
                                              @PathVariable Long id,
                                              @Valid @RequestBody SetAdminRolesRequest req) {
        return ApiResponse.ok(roleManagementService.setRoles(principal.id(), id, req));
    }

    @PostMapping("/admins/{id}/status")
    public ApiResponse<AdminUserDto> updateStatus(@AuthenticationPrincipal UserPrincipal principal,
                                                  @PathVariable Long id,
                                                  @RequestBody Map<String, String> body) {
        return ApiResponse.ok(roleManagementService.updateStatus(principal.id(), id, body.get("status")));
    }
}
