package com.jaram.auth.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

/**
 * 무상태 JWT 발급·검증(HS256). 짧은 만료(~1h), 리프레시 없음(NFR-6).
 * subject = teacherId, claim "email".
 */
@Component
public class JwtProvider {

    private static final String CLAIM_EMAIL = "email";

    private final SecretKey key;
    private final long expirationSeconds;

    public JwtProvider(JwtProperties properties) {
        this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
        this.expirationSeconds = properties.expirationSeconds();
    }

    public String createToken(Long teacherId, String email) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(expirationSeconds);
        return Jwts.builder()
                .subject(String.valueOf(teacherId))
                .claim(CLAIM_EMAIL, email)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(key)
                .compact();
    }

    /**
     * 토큰 파싱 후 teacherId(subject) 반환.
     *
     * @throws io.jsonwebtoken.ExpiredJwtException 만료 시
     * @throws io.jsonwebtoken.JwtException        서명/형식 오류 시
     */
    public Long parseTeacherId(String token) {
        Claims claims = parseClaims(token);
        return Long.valueOf(claims.getSubject());
    }

    public long getExpirationSeconds() {
        return expirationSeconds;
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
