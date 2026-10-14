package com.aijobhunter.controller;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.EnabledIf;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 认证 Live API 测试：打<strong>已启动</strong>的后端（默认 IDEA 本机 {@code http://127.0.0.1:8080}）。
 *
 * <p><b>不</b>启动 Docker / Testcontainers / 第二套 Spring 进程。
 * 请先用 IDEA 跑起 {@code AiJobHunterApplication}，再执行：
 * <pre>
 * ./gradlew test --tests com.aijobhunter.controller.AuthApiLiveIT
 * </pre>
 * 也可用环境变量覆盖：{@code LIVE_API_BASE=http://127.0.0.1:8080}
 *
 * <p>对齐 docs/09-TEST-STRATEGY.md AUTH-001～006（按现实现校正预期）。
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@EnabledIf("com.aijobhunter.controller.AuthApiLiveIT#backendUp")
class AuthApiLiveIT {

    private static final String BASE = System.getenv().getOrDefault("LIVE_API_BASE", "http://127.0.0.1:8080");
    private static final String PASSWORD = "pass1234";
    private static final String USERNAME = "auth_live_" + UUID.randomUUID().toString().substring(0, 8);
    private static final String EMAIL = USERNAME + "@example.com";

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    private static final Pattern CODE = Pattern.compile("\"code\"\\s*:\\s*(-?\\d+)");
    private static final Pattern MESSAGE = Pattern.compile("\"message\"\\s*:\\s*\"([^\"]*)\"");
    private static final Pattern TOKEN = Pattern.compile("\"token\"\\s*:\\s*\"([^\"]+)\"");

    /** JUnit {@link EnabledIf} 入口：后端未起则整类跳过，避免误开 Docker。 */
    static boolean backendUp() {
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(BASE + "/actuator/health"))
                    .timeout(Duration.ofSeconds(2))
                    .GET()
                    .build();
            HttpResponse<String> res = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
            return res.statusCode() == 200 && res.body() != null && res.body().contains("UP");
        } catch (Exception e) {
            return false;
        }
    }

    @BeforeAll
    static void banner() {
        System.out.println("[AuthApiLiveIT] LIVE_API_BASE=" + BASE + " user=" + USERNAME);
    }

    @Test
    @Order(1)
    @DisplayName("AUTH-001 正常注册：成功且响应体不含 token")
    void auth001_registerSuccessWithoutToken() throws Exception {
        HttpResponse<String> res = postJson("/api/v1/auth/register", """
                {"username":"%s","email":"%s","password":"%s"}
                """.formatted(USERNAME, EMAIL, PASSWORD));

        assertThat(res.statusCode()).isEqualTo(200);
        assertThat(code(res.body())).isEqualTo(0);
        assertThat(message(res.body())).contains("注册成功");
        assertThat(res.body()).contains("\"username\"");
        assertThat(res.body()).doesNotContain("\"token\"");
    }

    @Test
    @Order(2)
    @DisplayName("AUTH-002 邮箱已存在")
    void auth002_duplicateEmail() throws Exception {
        HttpResponse<String> res = postJson("/api/v1/auth/register", """
                {"username":"%s","email":"%s","password":"%s"}
                """.formatted(USERNAME + "_other", EMAIL, PASSWORD));

        assertThat(res.statusCode()).isEqualTo(400);
        assertThat(code(res.body())).isEqualTo(3002);
        assertThat(message(res.body())).contains("邮箱已被注册");
    }

    @Test
    @Order(3)
    @DisplayName("AUTH-003 短密码：当前无服务端长度校验，可注册")
    void auth003_shortPasswordAllowed() throws Exception {
        String shortUser = "short_" + UUID.randomUUID().toString().substring(0, 6);
        HttpResponse<String> res = postJson("/api/v1/auth/register", """
                {"username":"%s","email":"%s","password":"123456"}
                """.formatted(shortUser, shortUser + "@example.com"));

        assertThat(res.statusCode()).isEqualTo(200);
        assertThat(code(res.body())).isEqualTo(0);
    }

    @Test
    @Order(4)
    @DisplayName("AUTH-004 邮箱或用户名登录均返回 token")
    void auth004_loginWithEmailOrUsername() throws Exception {
        HttpResponse<String> byEmail = postJson("/api/v1/auth/login", """
                {"account":"%s","password":"%s"}
                """.formatted(EMAIL, PASSWORD));
        assertThat(byEmail.statusCode()).isEqualTo(200);
        assertThat(code(byEmail.body())).isEqualTo(0);
        assertThat(token(byEmail.body())).isNotBlank();

        HttpResponse<String> byUser = postJson("/api/v1/auth/login", """
                {"account":"%s","password":"%s"}
                """.formatted(USERNAME, PASSWORD));
        assertThat(byUser.statusCode()).isEqualTo(200);
        assertThat(code(byUser.body())).isEqualTo(0);
        assertThat(token(byUser.body())).isNotBlank();
    }

    @Test
    @Order(5)
    @DisplayName("AUTH-005 密码错误")
    void auth005_wrongPassword() throws Exception {
        HttpResponse<String> res = postJson("/api/v1/auth/login", """
                {"account":"%s","password":"wrong-password"}
                """.formatted(EMAIL));

        assertThat(res.statusCode()).isEqualTo(400);
        assertThat(code(res.body())).isEqualTo(2001);
        assertThat(message(res.body())).contains("用户名/邮箱或密码错误");
    }

    @Test
    @Order(6)
    @DisplayName("AUTH-006 无效/过期 Token 访问受保护接口 → 4xx")
    void auth006_badTokenRejected() throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(BASE + "/api/v1/jobs"))
                .timeout(Duration.ofSeconds(5))
                .header("Authorization", "Bearer eyJhbGciOiJIUzI1NiJ9.eyJleHAiOjF9.invalid")
                .GET()
                .build();
        HttpResponse<String> res = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
        assertThat(res.statusCode()).isBetween(400, 499);
    }

    private static HttpResponse<String> postJson(String path, String json) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(BASE + path))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        return CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
    }

    private static int code(String body) {
        Matcher m = CODE.matcher(body);
        assertThat(m.find()).as("body has code: %s", body).isTrue();
        return Integer.parseInt(m.group(1));
    }

    private static String message(String body) {
        Matcher m = MESSAGE.matcher(body);
        assertThat(m.find()).as("body has message: %s", body).isTrue();
        return m.group(1);
    }

    private static String token(String body) {
        Matcher m = TOKEN.matcher(body);
        assertThat(m.find()).as("body has token: %s", body).isTrue();
        return m.group(1);
    }
}
