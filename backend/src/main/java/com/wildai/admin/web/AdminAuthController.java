package com.wildai.admin.web;

import com.wildai.admin.domain.AdminUser;
import com.wildai.admin.repository.AdminUserRepository;
import com.wildai.common.dto.ApiResponse;
import com.wildai.common.exception.BusinessException;
import com.wildai.common.exception.ErrorCode;
import com.wildai.common.security.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin/api/auth")
public class AdminAuthController {

    private final AdminUserRepository adminRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AdminAuthController(AdminUserRepository adminRepo, PasswordEncoder passwordEncoder,
                               JwtTokenProvider jwtTokenProvider) {
        this.adminRepo = adminRepo;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @PostMapping("/login")
    public ApiResponse<Map<String, String>> login(@RequestBody Map<String, String> body) {
        AdminUser admin = adminRepo.findByUsername(body.get("username"))
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "用户名或密码错误"));
        if (!passwordEncoder.matches(body.get("password"), admin.getPasswordHash())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户名或密码错误");
        }
        String token = jwtTokenProvider.createAdminAccessToken(admin.getId(), admin.getUsername());
        return ApiResponse.ok(Map.of("accessToken", token, "username", admin.getUsername()));
    }
}
