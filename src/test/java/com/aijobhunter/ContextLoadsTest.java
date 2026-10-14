package com.aijobhunter;

import com.aijobhunter.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/**
 * Spring 上下文冒烟测试（CI / 本机显式开 Docker 时跑）。
 *
 * <p>依赖 Testcontainers 起 PostgreSQL + Redis，<strong>不会</strong>在日常 IDEA 联调时自动执行：
 * 需设置环境变量 {@code RUN_TESTCONTAINERS=true}（GitHub Actions CI 可配置）。
 *
 * <p>日常本地验证认证：用 IDEA 启动应用后跑 {@code AuthApiLiveIT}（不启 Docker）。
 */
@EnabledIfEnvironmentVariable(named = "RUN_TESTCONTAINERS", matches = "true")
class ContextLoadsTest extends AbstractIntegrationTest {

    @Test
    void contextLoads() {
        // 走到这里就说明 Spring 上下文能起、所有 bean 注入正确
    }
}
