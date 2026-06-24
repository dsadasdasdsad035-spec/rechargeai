package com.wildai.admin.repository;

import com.wildai.admin.domain.AdminUserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AdminUserRoleRepository extends JpaRepository<AdminUserRole, AdminUserRole.AdminUserRoleId> {

    @Query("""
            SELECT r.roleCode FROM AdminRole r
            INNER JOIN AdminUserRole ur ON ur.roleId = r.id
            WHERE ur.adminUserId = :adminUserId
            """)
    List<String> findRoleCodesByAdminUserId(@Param("adminUserId") Long adminUserId);

    boolean existsByAdminUserIdAndRoleId(Long adminUserId, Long roleId);

    void deleteByAdminUserId(Long adminUserId);

    List<AdminUserRole> findByAdminUserId(Long adminUserId);
}
