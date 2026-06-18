package com.ondo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

/**
 * JPA Auditing 활성화. 저장 시각은 UTC 로 통일한다(data-model-spec §0/§6).
 */
@Configuration
@EnableJpaAuditing(dateTimeProviderRef = "utcDateTimeProvider")
public class JpaConfig {

    @Bean
    public DateTimeProvider utcDateTimeProvider() {
        return () -> Optional.of(LocalDateTime.now(ZoneOffset.UTC));
    }
}
