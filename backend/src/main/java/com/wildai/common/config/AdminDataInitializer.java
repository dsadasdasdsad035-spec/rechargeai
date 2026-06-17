package com.wildai.common.config;

import com.wildai.admin.domain.AdminUser;
import com.wildai.admin.repository.AdminUserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AdminDataInitializer {

    @Bean
    CommandLineRunner initAdmin(AdminUserRepository repo, PasswordEncoder encoder) {
        return args -> {
            if (repo.findByUsername("admin").isEmpty()) {
                AdminUser admin = new AdminUser();
                admin.setUsername("admin");
                admin.setPasswordHash(encoder.encode("changeme"));
                admin.setDisplayName("超级管理员");
                admin.setStatus("ACTIVE");
                repo.save(admin);
            }
        };
    }
}
