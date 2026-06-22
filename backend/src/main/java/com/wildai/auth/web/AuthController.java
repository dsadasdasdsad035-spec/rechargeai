package com.wildai.auth.web;

import com.wildai.auth.dto.LoginRequest;
import com.wildai.auth.dto.RegisterRequest;
import com.wildai.auth.dto.TokenResponse;
import com.wildai.auth.service.AuthService;
import com.wildai.auth.service.VerifyCodeService;
import com.wildai.common.dto.ApiResponse;
import com.wildai.common.exception.BusinessException;
import com.wildai.common.exception.ErrorCode;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
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
        String email = body.get("email");
        String target;
        if (email != null && !email.isBlank()) {
            verifyCodeService.requireEmail(email);
            target = email.trim();
        } else if (phone != null && !phone.isBlank()) {
            target = phone.trim();
        } else {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请提供手机号或邮箱");
        }

        String code = verifyCodeService.sendCode(target);
        Map<String, String> result = new HashMap<>();
        result.put("message", verifyCodeService.isEmail(target) ? "验证码已发送至邮箱" : "验证码已发送");
        if (verifyCodeService.exposeDevCode()) {
            result.put("devCode", code);
        }
        return ApiResponse.ok(result);
    }
}
