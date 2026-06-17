package com.wildai.auth.web;

import com.wildai.auth.dto.LoginRequest;
import com.wildai.auth.dto.RegisterRequest;
import com.wildai.auth.dto.TokenResponse;
import com.wildai.auth.service.AuthService;
import com.wildai.auth.service.VerifyCodeService;
import com.wildai.common.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final VerifyCodeService verifyCodeService;

    public AuthController(AuthService authService, VerifyCodeService verifyCodeService) {
        this.authService = authService;
        this.verifyCodeService = verifyCodeService;
    }

    @PostMapping("/register")
    public ApiResponse<TokenResponse> register(@Valid @RequestBody RegisterRequest req) {
        return ApiResponse.ok(authService.register(req));
    }

    @PostMapping("/login")
    public ApiResponse<TokenResponse> login(@RequestBody LoginRequest req) {
        return ApiResponse.ok(authService.login(req));
    }

    @PostMapping("/refresh")
    public ApiResponse<TokenResponse> refresh(@RequestBody Map<String, String> body) {
        return ApiResponse.ok(authService.refresh(body.get("refreshToken")));
    }

    @PostMapping("/send-code")
    public ApiResponse<Map<String, String>> sendCode(@RequestBody Map<String, String> body) {
        String phone = body.get("phone");
        String code = verifyCodeService.sendCode(phone);
        // 开发环境返回验证码，生产应通过短信发送
        return ApiResponse.ok(Map.of("message", "验证码已发送", "devCode", code));
    }
}
