package com.aijobhunter.common.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JWT 纯单元测试（不启 Spring / Docker），保证 CI unit job 有真实断言。
 */
class JwtTokenProviderTest {

    private JwtTokenProvider provider;

    @BeforeEach
    void setUp() {
        provider = new JwtTokenProvider(
                "test-jwt-secret-at-least-32-characters-long-for-testing",
                86_400_000L);
    }

    @Test
    @DisplayName("生成的 token 可解析出同一 userId")
    void generateAndParse() {
        String token = provider.generateToken("user-123");
        assertThat(token).isNotBlank();
        assertThat(provider.validateToken(token)).isTrue();
        assertThat(provider.getUserIdFromToken(token)).isEqualTo("user-123");
    }

    @Test
    @DisplayName("非法 token 校验失败")
    void invalidToken() {
        assertThat(provider.validateToken("not.a.jwt")).isFalse();
        assertThat(provider.validateToken("")).isFalse();
        // 用另一把密钥签发，当前 provider 应拒绝
        JwtTokenProvider other = new JwtTokenProvider(
                "another-jwt-secret-at-least-32-characters-xxxxx",
                86_400_000L);
        assertThat(provider.validateToken(other.generateToken("user-123"))).isFalse();
    }

    @Test
    @DisplayName("expiration 秒级可读")
    void expiration() {
        assertThat(provider.getExpiration()).isEqualTo(86_400_000L);
    }
}
