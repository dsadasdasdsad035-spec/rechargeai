package com.wildai.auth.service;

import com.wildai.auth.domain.UserAccount;
import com.wildai.auth.dto.LoginRequest;
import com.wildai.auth.dto.RegisterRequest;
import com.wildai.auth.dto.TokenResponse;
import com.wildai.auth.repository.UserAccountRepository;
import com.wildai.common.exception.BusinessException;
import com.wildai.common.exception.ErrorCode;
import com.wildai.common.ratelimit.RateLimitService;
import com.wildai.common.security.JwtTokenProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.UUID;

@Service
public class AuthService {

    private final UserAccountRepository userRepo;
    private final JwtTokenProvider jwtTokenProvider;
    private final VerifyCodeService verifyCodeService;
    private final RateLimitService rateLimitService;

    public AuthService(UserAccountRepository userRepo,
                       JwtTokenProvider jwtTokenProvider,
                       VerifyCodeService verifyCodeService,
                       RateLimitService rateLimitService) {
        this.userRepo = userRepo;
        this.jwtTokenProvider = jwtTokenProvider;
        this.verifyCodeService = verifyCodeService;
        this.rateLimitService = rateLimitService;
    }

    @Transactional
    public TokenResponse register(RegisterRequest req) {
        if (req.email() == null || req.email().isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "邮箱不能为空");
        }
        verifyCodeService.requireEmail(req.email());
        if (!verifyCodeService.verify(req.email(), req.verifyCode())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "验证码错误");
        }
        if (userRepo.findByEmail(req.email()).isPresent()) {
            throw new BusinessException(ErrorCode.CONFLICT, "邮箱已注册");
        }
        UserAccount user = new UserAccount();
        user.setUserNo("U" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        user.setEmail(req.email());
        user.setNickname(req.email().split("@")[0]);
        userRepo.save(user);
        return tokens(user);
    }

    public TokenResponse login(LoginRequest req) {
        if (req.email() == null || req.email().isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "邮箱不能为空");
        }
        verifyCodeService.requireEmail(req.email());
        rateLimitService.check("login:" + req.email(), 10, Duration.ofMinutes(10));

        if (req.verifyCode() == null || req.verifyCode().isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "验证码不能为空");
        }
        if (!verifyCodeService.verify(req.email(), req.verifyCode())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "验证码错误");
        }

        UserAccount user = userRepo.findByEmail(req.email())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "用户不存在，请先注册"));
        ensureActive(user);
        return tokens(user);
    }

    public TokenResponse refresh(String refreshToken) {
        var claims = jwtTokenProvider.parse(refreshToken);
        if (!"refresh".equals(claims.get("type", String.class))) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        Long userId = Long.parseLong(claims.getSubject());
        UserAccount user = userRepo.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        ensureActive(user);
        return tokens(user);
    }

    private TokenResponse tokens(UserAccount user) {
        return new TokenResponse(
                jwtTokenProvider.createUserAccessToken(user.getId(), user.getUserNo()),
                jwtTokenProvider.createUserRefreshToken(user.getId()),
                user.getUserNo());
    }

    private void ensureActive(UserAccount user) {
        if (!"NORMAL".equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
        }
    }
}
