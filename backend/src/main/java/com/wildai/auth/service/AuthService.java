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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.UUID;

@Service
public class AuthService {

    private final UserAccountRepository userRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final VerifyCodeService verifyCodeService;
    private final RateLimitService rateLimitService;

    public AuthService(UserAccountRepository userRepo, PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider, VerifyCodeService verifyCodeService,
                       RateLimitService rateLimitService) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.verifyCodeService = verifyCodeService;
        this.rateLimitService = rateLimitService;
    }

    @Transactional
    public TokenResponse register(RegisterRequest req) {
        if ("PHONE".equalsIgnoreCase(req.type())) {
            if (req.phone() == null || req.phone().isBlank()) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "手机号不能为空");
            }
            if (!verifyCodeService.verify(req.phone(), req.verifyCode())) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "验证码错误");
            }
            if (userRepo.findByPhone(req.phone()).isPresent()) {
                throw new BusinessException(ErrorCode.CONFLICT, "手机号已注册");
            }
            UserAccount user = new UserAccount();
            user.setUserNo("U" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
            user.setPhone(req.phone());
            user.setNickname("用户" + req.phone().substring(req.phone().length() - 4));
            userRepo.save(user);
            return tokens(user);
        }
        if (req.email() == null || req.password() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "邮箱和密码不能为空");
        }
        if (userRepo.findByEmail(req.email()).isPresent()) {
            throw new BusinessException(ErrorCode.CONFLICT, "邮箱已注册");
        }
        UserAccount user = new UserAccount();
        user.setUserNo("U" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        user.setEmail(req.email());
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.setNickname(req.email().split("@")[0]);
        userRepo.save(user);
        return tokens(user);
    }

    public TokenResponse login(LoginRequest req) {
        String key = req.phone() != null ? req.phone() : req.email();
        rateLimitService.check("login:" + key, 10, Duration.ofMinutes(10));

        UserAccount user;
        if (req.phone() != null && !req.phone().isBlank()) {
            if (!verifyCodeService.verify(req.phone(), req.verifyCode())) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "验证码错误");
            }
            user = userRepo.findByPhone(req.phone()).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "用户不存在"));
        } else {
            user = userRepo.findByEmail(req.email()).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "用户不存在"));
            if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
                throw new BusinessException(ErrorCode.UNAUTHORIZED, "密码错误");
            }
        }
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
