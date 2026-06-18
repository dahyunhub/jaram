package com.ondo.auth.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT 설정. application.yml 의 ondo.jwt.* 바인딩.
 *
 * @param secret            HS256 서명 키(최소 32바이트). 운영은 env(JWT_SECRET)로 주입.
 * @param expirationSeconds 액세스 토큰 만료(초). 리프레시 없음(재로그인), 짧게(~1h).
 */
@ConfigurationProperties(prefix = "ondo.jwt")
public record JwtProperties(String secret, long expirationSeconds) {
}
