package com.aijobhunter;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Spring 上下文冒烟测试。
 *
 * <p>目的：CI 强制启动 Spring 上下文，校验所有 {@code @Service}/{@code @Configuration}
 * 的 Bean 都能成功注入。一旦出现 bean 缺失、循环依赖、构造器注入歧义等问题，
 * 这个测试会立即失败，CD 不会被触发。
 *
 * <p>环境：使用 Testcontainers 启动真实的 PostgreSQL + Redis 容器，
 * 与生产环境完全等价（不是 H2 / embedded-redis）。
 *
 * <p>历史背景：2026-09-22 部署失败，{@code ChatAgent} 启动时报
 * {@code Parameter 1 of constructor required a bean of type 'ObjectMapper'}，
 * CI 跑空 {@code ./gradlew test} 直接通过，导致错误逃逸到生产环境。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ContextLoadsTest {

    /**
     * 真实 PostgreSQL 容器（生产环境：postgres:18-alpine）。
     * schema-test.sql 会初始化 app schema。
     */
    @SuppressWarnings("resource")
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:18-alpine")
                    .asCompatibleSubstituteFor("postgres"))
            .withDatabaseName("job_hunter")
            .withUsername("postgres")
            .withPassword("postgres")
            .withInitScript("schema-test.sql");

    /**
     * 真实 Redis 容器（生产环境：redis:7-alpine）。
     */
    @SuppressWarnings("resource")
    static GenericContainer<?> redis = new GenericContainer<>(
            DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    static {
        postgres.start();
        redis.start();
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        // PostgreSQL（与 application.yml 中的生产配置完全等价）
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        // Redis
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));

        // JWT（测试用占位符，不会真签发 token）
        registry.add("jwt.secret",
                () -> "test-jwt-secret-at-least-32-characters-long-for-testing");

        // DeepSeek API（测试用占位符，不会真发请求）
        registry.add("spring.ai.deepseek.api-key", () -> "test-dummy-key-not-real");
    }

    @Test
    void contextLoads() {
        // 走到这里就说明 Spring 上下文能起、所有 bean 注入正确
    }
}
