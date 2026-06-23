package com.wildai.admin.web;

import com.wildai.admin.domain.AdminUser;
import com.wildai.admin.repository.AdminUserRepository;
import com.wildai.admin.service.RbacService;
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
    private final RbacService rbacService;

    public AdminAuthController(AdminUserRepository adminRepo, PasswordEncoder passwordEncoder,
                               JwtTokenProvider jwtTokenProvider, RbacService rbacService) {
        this.adminRepo = adminRepo;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.rbacService = rbacService;
    }

    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        AdminUser admin = adminRepo.findByUsername(body.get("username"))
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "用户名或密码错误"));
        if (!passwordEncoder.matches(body.get("password"), admin.getPasswordHash())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户名或密码错误");
        }
        var roles = rbacService.getRoleCodes(admin.getId());
        String token = jwtTokenProvider.createAdminAccessToken(admin.getId(), admin.getUsername(), roles);
        return ApiResponse.ok(Map.of(
                "accessToken", token,
                "username", admin.getUsername(),
                "roles", roles
        ));
    }
}
