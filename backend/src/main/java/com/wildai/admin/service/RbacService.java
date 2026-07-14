package com.wildai.admin.service;

import com.wildai.admin.domain.AdminRole;
import com.wildai.admin.domain.AdminUserRole;
import com.wildai.admin.repository.AdminRoleRepository;
import com.wildai.admin.repository.AdminUserRoleRepository;
import com.wildai.common.exception.BusinessException;
import com.wildai.common.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
public class RbacService {

    private static final Set<String> REFUND_APPROVER_ROLES = Set.of(
            AdminRoleCode.SUPER_ADMIN,
            AdminRoleCode.OPS_ADMIN,
            AdminRoleCode.FINANCE
    );

    private final AdminUserRoleRepository userRoleRepo;
    private final AdminRoleRepository roleRepo;

    public RbacService(AdminUserRoleRepository userRoleRepo, AdminRoleRepository roleRepo) {
        this.userRoleRepo = userRoleRepo;
        this.roleRepo = roleRepo;
    }

    public List<String> getRoleCodes(Long adminUserId) {
        return userRoleRepo.findRoleCodesByAdminUserId(adminUserId);
    }

    public boolean hasAnyRole(Long adminUserId, String... roleCodes) {
        List<String> roles = getRoleCodes(adminUserId);
        for (String code : roleCodes) {
            if (roles.contains(code)) {
                return true;
            }
        }
        return false;
    }

    public boolean canApproveRefund(Long adminUserId) {
        return hasAnyRole(adminUserId,
                AdminRoleCode.SUPER_ADMIN,
                AdminRoleCode.OPS_ADMIN,
                AdminRoleCode.FINANCE);
    }

    public boolean isAuditReadonlyOnly(Long adminUserId) {
        List<String> roles = getRoleCodes(adminUserId);
        return !roles.isEmpty() && roles.stream().allMatch(AdminRoleCode.AUDIT_READONLY::equals);
    }

    /** 客服、运营、超管可执行履约写操作；财务与只读审计仅可查看 */
    public boolean canManageFulfillment(Long adminUserId) {
        return hasAnyRole(adminUserId,
                AdminRoleCode.SUPER_ADMIN,
                AdminRoleCode.OPS_ADMIN,
                AdminRoleCode.CUSTOMER_SERVICE);
    }

    public boolean isSuperAdmin(Long adminUserId) {
        return hasAnyRole(adminUserId, AdminRoleCode.SUPER_ADMIN);
    }

    public boolean canManageAdmins(Long adminUserId) {
        return isSuperAdmin(adminUserId);
    }

    public boolean canViewAuditLogs(Long adminUserId) {
        return hasAnyRole(adminUserId,
                AdminRoleCode.SUPER_ADMIN,
                AdminRoleCode.OPS_ADMIN,
                AdminRoleCode.AUDIT_READONLY);
    }

    public boolean canForceOrderStatus(Long adminUserId) {
        return hasAnyRole(adminUserId,
                AdminRoleCode.SUPER_ADMIN,
                AdminRoleCode.OPS_ADMIN);
    }

    public boolean canManageContent(Long adminUserId) {
        return hasAnyRole(adminUserId,
                AdminRoleCode.SUPER_ADMIN,
                AdminRoleCode.OPS_ADMIN);
    }

    /** 财务、超管可操作资金；只读审计仅查看 */
    public boolean canViewFinance(Long adminUserId) {
        return hasAnyRole(adminUserId,
                AdminRoleCode.SUPER_ADMIN,
                AdminRoleCode.FINANCE,
                AdminRoleCode.AUDIT_READONLY);
    }

    public boolean canManageFinance(Long adminUserId) {
        return hasAnyRole(adminUserId,
                AdminRoleCode.SUPER_ADMIN,
                AdminRoleCode.FINANCE);
    }

    public void requireViewFinance(Long adminUserId) {
        if (!canViewFinance(adminUserId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权查看资金信息");
        }
    }

    public void requireManageFinance(Long adminUserId) {
        if (!canManageFinance(adminUserId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作资金");
        }
    }

    public void requireManageAdmins(Long adminUserId) {
        if (!canManageAdmins(adminUserId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅超级管理员可执行此操作");
        }
    }

    public void requireManageContent(Long adminUserId) {
        if (!canManageContent(adminUserId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权管理文章内容");
        }
    }

    @Transactional
    public void assignRoleIfAbsent(Long adminUserId, String roleCode) {
        AdminRole role = roleRepo.findByRoleCode(roleCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_ERROR, "角色未初始化: " + roleCode));
        if (!userRoleRepo.existsByAdminUserIdAndRoleId(adminUserId, role.getId())) {
            AdminUserRole link = new AdminUserRole();
            link.setAdminUserId(adminUserId);
            link.setRoleId(role.getId());
            userRoleRepo.save(link);
        }
    }
}
