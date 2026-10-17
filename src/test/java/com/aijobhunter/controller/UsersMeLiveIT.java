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
 * 用户信息 Live API：GET/PUT /api/v1/users/me（需 IDEA/本机已启动后端）。
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@EnabledIf("com.aijobhunter.controller.UsersMeLiveIT#backendUp")
class UsersMeLiveIT {

    private static final String BASE = System.getenv().getOrDefault("LIVE_API_BASE", "http://127.0.0.1:8080");
    private static final String USERNAME = "me_" + UUID.randomUUID().toString().substring(0, 8);
    private static final String EMAIL = USERNAME + "@example.com";
    private static final String PASSWORD = "pass1234";

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    private static final Pattern CODE = Pattern.compile("\"code\"\\s*:\\s*(-?\\d+)");
    private static final Pattern TOKEN = Pattern.compile("\"token\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern USERNAME_JSON = Pattern.compile("\"username\"\\s*:\\s*\"([^\"]+)\"");

    private static String token;

    static boolean backendUp() {
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(BASE + "/actuator/health"))
                    .timeout(Duration.ofSeconds(2)).GET().build();
            HttpResponse<String> res = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
            return res.statusCode() == 200 && res.body() != null && res.body().contains("UP");
        } catch (Exception e) {
            return false;
        }
    }

    @BeforeAll
    static void registerAndLogin() throws Exception {
        postJson("/api/v1/auth/register", """
                {"username":"%s","email":"%s","password":"%s"}
                """.formatted(USERNAME, EMAIL, PASSWORD));
        HttpResponse<String> login = postJson("/api/v1/auth/login", """
                {"account":"%s","password":"%s"}
                """.formatted(EMAIL, PASSWORD));
        assertThat(code(login.body())).isEqualTo(0);
        token = token(login.body());
        assertThat(token).isNotBlank();
    }

    @Test
    @Order(1)
    @DisplayName("USER-001 GET /users/me 返回当前用户")
    void getMe() throws Exception {
        HttpResponse<String> res = getAuth("/api/v1/users/me");
        assertThat(res.statusCode()).isEqualTo(200);
        assertThat(code(res.body())).isEqualTo(0);
        Matcher m = USERNAME_JSON.matcher(res.body());
        assertThat(m.find()).isTrue();
        assertThat(m.group(1)).isEqualTo(USERNAME);
        assertThat(res.body()).contains(EMAIL);
    }

    @Test
    @Order(2)
    @DisplayName("USER-002 PUT /users/me 可更新 preferences")
    void updateMe() throws Exception {
        HttpResponse<String> res = putAuth("/api/v1/users/me", """
                {"phone":"13800001111","preferences":{"keywords":["Java"],"cities":["北京"]}}
                """);
        assertThat(res.statusCode()).isEqualTo(200);
        assertThat(code(res.body())).isEqualTo(0);
        assertThat(res.body()).contains("13800001111");
        assertThat(res.body()).contains("Java");
    }

    @Test
    @Order(3)
    @DisplayName("USER-003 无 Token 访问 /users/me → 4xx")
    void meUnauthorized() throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(BASE + "/api/v1/users/me"))
                .timeout(Duration.ofSeconds(5)).GET().build();
        HttpResponse<String> res = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
        assertThat(res.statusCode()).isBetween(400, 499);
    }

    private static HttpResponse<String> getAuth(String path) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(BASE + path))
                .timeout(Duration.ofSeconds(10))
                .header("Authorization", "Bearer " + token)
                .GET().build();
        return CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
    }

    private static HttpResponse<String> putAuth(String path, String json) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(BASE + path))
                .timeout(Duration.ofSeconds(10))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(json))
                .build();
        return CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
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

    private static String token(String body) {
        Matcher m = TOKEN.matcher(body);
        assertThat(m.find()).as("body has token: %s", body).isTrue();
        return m.group(1);
    }
}
