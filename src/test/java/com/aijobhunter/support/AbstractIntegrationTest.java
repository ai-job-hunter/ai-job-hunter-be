package com.aijobhunter.support;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * CI 集成测试基类（Testcontainers）。
 *
 * <p>容器只在 {@link BeforeAll} 启动，避免「测试类被加载但未执行」时误拉 Docker。
 * 日常本地认证请用 {@code AuthApiLiveIT} + IDEA 启后端，不要走本基类。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractIntegrationTest {

    @SuppressWarnings("resource")
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(
            DockerImageName.parse("postgres:18-alpine")
                    .asCompatibleSubstituteFor("postgres"))
            .withDatabaseName("job_hunter")
            .withUsername("postgres")
            .withPassword("postgres")
            .withInitScript("schema-test.sql");

    @SuppressWarnings("resource")
    static final GenericContainer<?> REDIS = new GenericContainer<>(
            DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @BeforeAll
    static void startContainers() {
        if (!POSTGRES.isRunning()) {
            POSTGRES.start();
        }
        if (!REDIS.isRunning()) {
            REDIS.start();
        }
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);

        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));

        registry.add("jwt.secret",
                () -> "test-jwt-secret-at-least-32-characters-long-for-testing");
        registry.add("jwt.expiration", () -> "86400000");

        registry.add("spring.ai.deepseek.api-key", () -> "test-dummy-key-not-real");
    }
}
