package com.jaram.config;

import com.jaram.ai.AiProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * AI 설정 활성화. OpenAiClient 는 @Component 로 AiProperties 를 주입받는다.
 */
@Configuration
@EnableConfigurationProperties(AiProperties.class)
public class AiClientConfig {
}
