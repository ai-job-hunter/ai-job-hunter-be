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
 * V2 阶段 A：平台 MOCK 连接 → 建任务 → 确认完成（需本机已启后端）。
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@EnabledIf("com.aijobhunter.controller.PlatformTaskLiveIT#backendUp")
class PlatformTaskLiveIT {

    private static final String BASE = System.getenv().getOrDefault("LIVE_API_BASE", "http://127.0.0.1:8080");
    private static final String USERNAME = "plat_" + UUID.randomUUID().toString().substring(0, 8);
    private static final String EMAIL = USERNAME + "@example.com";
    private static final String PASSWORD = "pass1234";

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    private static final Pattern CODE = Pattern.compile("\"code\"\\s*:\\s*(-?\\d+)");
    private static final Pattern TOKEN = Pattern.compile("\"token\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern ID = Pattern.compile("\"id\"\\s*:\\s*\"([^\"]+)\"");

    private static String token;
    private static String connectionId;
    private static String taskId;

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
    @DisplayName("PLAT-001 创建 MOCK 连接")
    void createMockConnection() throws Exception {
        HttpResponse<String> res = postAuth("/api/v1/platforms/connections", """
                {"platform":"MOCK"}
                """);
        assertThat(res.statusCode()).isEqualTo(200);
        assertThat(code(res.body())).isEqualTo(0);
        assertThat(res.body()).contains("CONNECTED");
        connectionId = firstId(res.body());
        assertThat(connectionId).isNotBlank();
    }

    @Test
    @Order(2)
    @DisplayName("PLAT-002 真实平台占位拒绝")
    void rejectBossConnection() throws Exception {
        HttpResponse<String> res = postAuth("/api/v1/platforms/connections", """
                {"platform":"BOSS"}
                """);
        assertThat(res.statusCode()).isEqualTo(400);
        assertThat(res.body()).contains("尚未开放");
    }

    @Test
    @Order(3)
    @DisplayName("PLAT-003 创建待确认导入任务（带 JD）")
    void createTask() throws Exception {
        HttpResponse<String> res = postAuth("/api/v1/platforms/tasks", """
                {
                  "connectionId":"%s",
                  "action":"SYNC_JOBS",
                  "mode":"SEMI_AUTO",
                  "title":"导入测试岗",
                  "companyName":"MockCorp",
                  "rawDescription":"负责 Java 后端开发，熟悉 Spring Boot。"
                }
                """.formatted(connectionId));
        assertThat(res.statusCode()).isEqualTo(200);
        assertThat(code(res.body())).isEqualTo(0);
        assertThat(res.body()).contains("NEED_CONFIRM");
        taskId = firstId(res.body());
        assertThat(taskId).isNotBlank();
    }

    @Test
    @Order(4)
    @DisplayName("PLAT-004 确认任务 → DONE 并导入岗位")
    void confirmTask() throws Exception {
        HttpResponse<String> res = postAuth("/api/v1/platforms/tasks/" + taskId + "/confirm", "{}");
        assertThat(res.statusCode()).isEqualTo(200);
        assertThat(code(res.body())).isEqualTo(0);
        assertThat(res.body()).contains("DONE");
        assertThat(res.body()).contains("jobId");
        assertThat(res.body()).contains("导入测试岗");

        HttpResponse<String> jobs = getAuth("/api/v1/jobs");
        assertThat(jobs.statusCode()).isEqualTo(200);
        assertThat(jobs.body()).contains("IMPORT");
        assertThat(jobs.body()).contains("导入测试岗");
    }

    private static String firstId(String body) {
        Matcher m = ID.matcher(body);
        assertThat(m.find()).as("body has id: %s", body).isTrue();
        return m.group(1);
    }

    private static HttpResponse<String> getAuth(String path) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(BASE + path))
                .timeout(Duration.ofSeconds(15))
                .header("Authorization", "Bearer " + token)
                .GET().build();
        return CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
    }

    private static HttpResponse<String> postAuth(String path, String json) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(BASE + path))
                .timeout(Duration.ofSeconds(30))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        return CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
    }

    private static HttpResponse<String> postJson(String path, String json) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(BASE + path))
                .timeout(Duration.ofSeconds(15))
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
