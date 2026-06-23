package com.wildai.admin.domain;

import jakarta.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "admin_user_role")
@IdClass(AdminUserRole.AdminUserRoleId.class)
public class AdminUserRole {

    @Id
    @Column(name = "admin_user_id")
    private Long adminUserId;

    @Id
    @Column(name = "role_id")
    private Long roleId;

    public Long getAdminUserId() { return adminUserId; }
    public void setAdminUserId(Long adminUserId) { this.adminUserId = adminUserId; }
    public Long getRoleId() { return roleId; }
    public void setRoleId(Long roleId) { this.roleId = roleId; }

    public static class AdminUserRoleId implements Serializable {
        private Long adminUserId;
        private Long roleId;

        public AdminUserRoleId() {}

        public AdminUserRoleId(Long adminUserId, Long roleId) {
            this.adminUserId = adminUserId;
            this.roleId = roleId;
        }

        public Long getAdminUserId() { return adminUserId; }
        public void setAdminUserId(Long adminUserId) { this.adminUserId = adminUserId; }
        public Long getRoleId() { return roleId; }
        public void setRoleId(Long roleId) { this.roleId = roleId; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof AdminUserRoleId that)) return false;
            return adminUserId.equals(that.adminUserId) && roleId.equals(that.roleId);
        }

        @Override
        public int hashCode() {
            return adminUserId.hashCode() * 31 + roleId.hashCode();
        }
    }
}
