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
