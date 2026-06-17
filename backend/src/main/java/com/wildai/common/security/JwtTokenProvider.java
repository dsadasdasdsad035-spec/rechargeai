package com.wildai.common.security;

import com.wildai.common.config.WildAiProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    public static final String AUD_USER = "user";
    public static final String AUD_ADMIN = "admin";

    private final SecretKey key;
    private final WildAiProperties properties;

    public JwtTokenProvider(WildAiProperties properties) {
        this.properties = properties;
        this.key = Keys.hmacShaKeyFor(properties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String createUserAccessToken(Long userId, String userNo) {
        Instant exp = Instant.now().plus(properties.getJwt().getUserAccessExpirationMinutes(), ChronoUnit.MINUTES);
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(String.valueOf(userId))
                .claim("userNo", userNo)
                .claim("aud", AUD_USER)
                .expiration(Date.from(exp))
                .signWith(key)
                .compact();
    }

    public String createUserRefreshToken(Long userId) {
        Instant exp = Instant.now().plus(properties.getJwt().getUserRefreshExpirationDays(), ChronoUnit.DAYS);
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(String.valueOf(userId))
                .claim("type", "refresh")
                .claim("aud", AUD_USER)
                .expiration(Date.from(exp))
                .signWith(key)
                .compact();
    }

    public String createAdminAccessToken(Long adminId, String username) {
        Instant exp = Instant.now().plus(properties.getJwt().getAdminAccessExpirationMinutes(), ChronoUnit.MINUTES);
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(String.valueOf(adminId))
                .claim("username", username)
                .claim("aud", AUD_ADMIN)
                .expiration(Date.from(exp))
                .signWith(key)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    public boolean isUserToken(Claims claims) {
        return AUD_USER.equals(claims.get("aud", String.class));
    }

    public boolean isAdminToken(Claims claims) {
        return AUD_ADMIN.equals(claims.get("aud", String.class));
    }
}
