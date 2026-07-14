package com.wildai.admin.service;

import com.wildai.admin.repository.AdminRoleRepository;
import com.wildai.admin.repository.AdminUserRoleRepository;
import com.wildai.common.exception.BusinessException;
import com.wildai.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RbacServiceTest {

    private final AdminUserRoleRepository userRoleRepository = mock(AdminUserRoleRepository.class);
    private final RbacService service = new RbacService(
            userRoleRepository, mock(AdminRoleRepository.class));

    @Test
    void contentManagementAllowsOnlySuperAdminAndOpsAdmin() {
        when(userRoleRepository.findRoleCodesByAdminUserId(1L))
                .thenReturn(List.of(AdminRoleCode.SUPER_ADMIN));
        when(userRoleRepository.findRoleCodesByAdminUserId(2L))
                .thenReturn(List.of(AdminRoleCode.OPS_ADMIN));
        when(userRoleRepository.findRoleCodesByAdminUserId(3L))
                .thenReturn(List.of(AdminRoleCode.AUDIT_READONLY));
        when(userRoleRepository.findRoleCodesByAdminUserId(4L))
                .thenReturn(List.of(AdminRoleCode.FINANCE));
        when(userRoleRepository.findRoleCodesByAdminUserId(5L))
                .thenReturn(List.of(AdminRoleCode.CUSTOMER_SERVICE));

        assertThat(service.canManageContent(1L)).isTrue();
        assertThat(service.canManageContent(2L)).isTrue();
        assertThat(service.canManageContent(3L)).isFalse();
        assertThat(service.canManageContent(4L)).isFalse();
        assertThat(service.canManageContent(5L)).isFalse();
    }

    @Test
    void contentManagementRequirementUsesForbiddenContract() {
        when(userRoleRepository.findRoleCodesByAdminUserId(9L))
                .thenReturn(List.of(AdminRoleCode.AUDIT_READONLY));

        assertThatThrownBy(() -> service.requireManageContent(9L))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN);
                    assertThat(exception).hasMessage("无权管理文章内容");
                });
    }
}
