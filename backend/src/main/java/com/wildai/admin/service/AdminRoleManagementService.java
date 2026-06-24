package com.wildai.admin.service;

import com.wildai.admin.domain.AdminUser;
import com.wildai.admin.dto.AdminRoleDto;
import com.wildai.admin.dto.AdminUserDto;
import com.wildai.admin.dto.CreateAdminRequest;
import com.wildai.admin.dto.SetAdminRolesRequest;
import com.wildai.admin.repository.AdminRoleRepository;
import com.wildai.admin.repository.AdminUserRepository;
import com.wildai.admin.repository.AdminUserRoleRepository;
import com.wildai.common.exception.BusinessException;
import com.wildai.common.exception.ErrorCode;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class AdminRoleManagementService {

    private final AdminRoleRepository roleRepo;
    private final AdminUserRepository adminUserRepo;
    private final AdminUserRoleRepository userRoleRepo;
    private final RbacService rbacService;
    private final AuditLogService auditLogService;
    private final PasswordEncoder passwordEncoder;

    public AdminRoleManagementService(AdminRoleRepository roleRepo,
                                      AdminUserRepository adminUserRepo,
                                      AdminUserRoleRepository userRoleRepo,
                                      RbacService rbacService,
                                      AuditLogService auditLogService,
                                      PasswordEncoder passwordEncoder) {
        this.roleRepo = roleRepo;
        this.adminUserRepo = adminUserRepo;
        this.userRoleRepo = userRoleRepo;
        this.rbacService = rbacService;
        this.auditLogService = auditLogService;
        this.passwordEncoder = passwordEncoder;
    }

    public List<AdminRoleDto> listRoles() {
        return roleRepo.findAll().stream()
                .map(r -> new AdminRoleDto(r.getId(), r.getRoleCode(), r.getRoleName()))
                .toList();
    }

    public List<AdminUserDto> listAdmins() {
        return adminUserRepo.findAll().stream().map(this::toAdminDto).toList();
    }

    @Transactional
    public AdminUserDto createAdmin(Long operatorId, CreateAdminRequest req) {
        requireSuperAdmin(operatorId);
        if (adminUserRepo.findByUsername(req.username()).isPresent()) {
            throw new BusinessException(ErrorCode.CONFLICT, "用户名已存在");
        }
        AdminUser admin = new AdminUser();
        admin.setUsername(req.username().trim());
        admin.setPasswordHash(passwordEncoder.encode(req.password()));
        admin.setDisplayName(req.displayName());
        admin.setStatus("ACTIVE");
        admin = adminUserRepo.save(admin);

        if (req.roleCodes() != null && !req.roleCodes().isEmpty()) {
            replaceRoles(admin.getId(), req.roleCodes());
        } else {
            rbacService.assignRoleIfAbsent(admin.getId(), AdminRoleCode.AUDIT_READONLY);
        }

        auditLogService.log(operatorId, "ADMIN_CREATE", "ADMIN_USER", String.valueOf(admin.getId()),
                null, admin.getUsername(), "创建管理员");

        return toAdminDto(admin);
    }

    @Transactional
    public AdminUserDto setRoles(Long operatorId, Long adminId, SetAdminRolesRequest req) {
        requireSuperAdmin(operatorId);
        AdminUser admin = adminUserRepo.findById(adminId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "管理员不存在"));
        if (req.roleCodes() == null || req.roleCodes().isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "至少分配一个角色");
        }

        List<String> before = rbacService.getRoleCodes(adminId);
        replaceRoles(adminId, req.roleCodes());

        auditLogService.log(operatorId, "ADMIN_SET_ROLES", "ADMIN_USER", String.valueOf(adminId),
                String.join(",", before), String.join(",", req.roleCodes()), "调整管理员角色");

        return toAdminDto(admin);
    }

    @Transactional
    public AdminUserDto updateStatus(Long operatorId, Long adminId, String status) {
        requireSuperAdmin(operatorId);
        if (operatorId.equals(adminId) && "DISABLED".equals(status)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不可禁用当前登录账号");
        }
        AdminUser admin = adminUserRepo.findById(adminId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "管理员不存在"));
        String before = admin.getStatus();
        admin.setStatus(status);
        admin.setUpdatedAt(Instant.now());
        adminUserRepo.save(admin);

        auditLogService.log(operatorId, "ADMIN_STATUS", "ADMIN_USER", String.valueOf(adminId),
                before, status, "变更管理员状态");

        return toAdminDto(admin);
    }

    private void replaceRoles(Long adminUserId, List<String> roleCodes) {
        userRoleRepo.deleteByAdminUserId(adminUserId);
        for (String code : roleCodes) {
            rbacService.assignRoleIfAbsent(adminUserId, code);
        }
    }

    private AdminUserDto toAdminDto(AdminUser admin) {
        return new AdminUserDto(
                admin.getId(),
                admin.getUsername(),
                admin.getDisplayName(),
                admin.getStatus(),
                rbacService.getRoleCodes(admin.getId())
        );
    }

    private void requireSuperAdmin(Long adminId) {
        if (!rbacService.canManageAdmins(adminId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅超级管理员可管理账号与角色");
        }
    }
}
