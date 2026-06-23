package com.wildai.common.config;

import com.wildai.admin.service.AdminRoleCode;
import com.wildai.admin.service.RbacService;
import com.wildai.admin.domain.AdminUser;
import com.wildai.admin.repository.AdminUserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AdminDataInitializer {

    @Bean
    CommandLineRunner initAdmin(AdminUserRepository repo, PasswordEncoder encoder, RbacService rbacService) {
        return args -> {
            AdminUser admin = repo.findByUsername("admin").orElseGet(() -> {
                AdminUser created = new AdminUser();
                created.setUsername("admin");
                created.setPasswordHash(encoder.encode("changeme"));
                created.setDisplayName("超级管理员");
                created.setStatus("ACTIVE");
                return repo.save(created);
            });
            rbacService.assignRoleIfAbsent(admin.getId(), AdminRoleCode.SUPER_ADMIN);
        };
    }
}
