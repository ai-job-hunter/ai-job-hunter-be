package com.aijobhunter.common.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Locale;

/**
 * 生产环境拒绝使用占位/弱 JWT，避免误部署后全站可伪造登录态。
 */
@Slf4j
@Component
public class ProdSecretValidator {

    private static final String WEAK_JWT = "your-256-bit-secret-key-here-must-be-at-least-32-chars";

    private final Environment environment;
    private final String jwtSecret;

    public ProdSecretValidator(
            Environment environment,
            @Value("${jwt.secret}") String jwtSecret) {
        this.environment = environment;
        this.jwtSecret = jwtSecret == null ? "" : jwtSecret.trim();
    }

    @PostConstruct
    void validate() {
        boolean prod = Arrays.stream(environment.getActiveProfiles())
                .anyMatch(p -> "prod".equalsIgnoreCase(p));
        if (!prod) {
            return;
        }
        if (jwtSecret.isBlank()
                || WEAK_JWT.equals(jwtSecret)
                || jwtSecret.toLowerCase(Locale.ROOT).contains("change_me")
                || jwtSecret.length() < 32) {
            throw new IllegalStateException(
                    "生产环境 JWT_SECRET 无效：请在服务器 .env 配置至少 32 字符的随机密钥（勿用默认值）");
        }
        log.info("Prod JWT secret check passed (length={})", jwtSecret.length());
    }
}
