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
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 简历 Live API：上传 / 列表 / 详情 / 更新 / 删除（需本机后端已启动）。
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@EnabledIf("com.aijobhunter.controller.ResumeLiveIT#backendUp")
class ResumeLiveIT {

    private static final String BASE = System.getenv().getOrDefault("LIVE_API_BASE", "http://127.0.0.1:8080");
    private static final String USERNAME = "res_" + UUID.randomUUID().toString().substring(0, 8);
    private static final String EMAIL = USERNAME + "@example.com";
    private static final String PASSWORD = "pass1234";
    private static final String BOUNDARY = "----AIJHBoundary" + UUID.randomUUID();

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    private static final Pattern CODE = Pattern.compile("\"code\"\\s*:\\s*(-?\\d+)");
    private static final Pattern TOKEN = Pattern.compile("\"token\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern ID = Pattern.compile("\"id\"\\s*:\\s*\"([0-9a-fA-F-]{36})\"");

    private static String token;
    private static String resumeId;

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
        token = token(login.body());
    }

    @Test
    @Order(1)
    @DisplayName("RES-001 上传 txt 简历并解析")
    void uploadTxt() throws Exception {
        String bodyText = "张三\nJava Spring Boot\n5年经验";
        byte[] body = multipartBody(
                "我的简历.txt",
                "text/plain",
                bodyText.getBytes(StandardCharsets.UTF_8),
                "技术简历V1",
                "TECHNICAL",
                "true");

        HttpRequest req = HttpRequest.newBuilder(URI.create(BASE + "/api/v1/resumes/upload"))
                .timeout(Duration.ofSeconds(20))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "multipart/form-data; boundary=" + BOUNDARY)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();
        HttpResponse<String> res = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());

        assertThat(res.statusCode()).isEqualTo(200);
        assertThat(code(res.body())).isEqualTo(0);
        assertThat(res.body()).contains("技术简历V1");
        assertThat(res.body()).contains("PARSED");
        resumeId = firstId(res.body());
        assertThat(resumeId).isNotBlank();
    }

    @Test
    @Order(2)
    @DisplayName("RES-002 简历列表包含刚上传项")
    void list() throws Exception {
        HttpResponse<String> res = getAuth("/api/v1/resumes");
        assertThat(res.statusCode()).isEqualTo(200);
        assertThat(code(res.body())).isEqualTo(0);
        assertThat(res.body()).contains(resumeId);
        assertThat(res.body()).contains("技术简历V1");
    }

    @Test
    @Order(3)
    @DisplayName("RES-003 简历详情")
    void detail() throws Exception {
        HttpResponse<String> res = getAuth("/api/v1/resumes/" + resumeId);
        assertThat(res.statusCode()).isEqualTo(200);
        assertThat(code(res.body())).isEqualTo(0);
        assertThat(res.body()).contains("rawText");
    }

    @Test
    @Order(4)
    @DisplayName("RES-004 更新简历名称")
    void update() throws Exception {
        HttpResponse<String> res = putAuth("/api/v1/resumes/" + resumeId, """
                {"name":"技术简历V2","isDefault":true}
                """);
        assertThat(res.statusCode()).isEqualTo(200);
        assertThat(code(res.body())).isEqualTo(0);
        assertThat(res.body()).contains("技术简历V2");
    }

    @Test
    @Order(5)
    @DisplayName("RES-005 删除简历")
    void delete() throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(BASE + "/api/v1/resumes/" + resumeId))
                .timeout(Duration.ofSeconds(10))
                .header("Authorization", "Bearer " + token)
                .DELETE().build();
        HttpResponse<String> res = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
        assertThat(res.statusCode()).isEqualTo(200);
        assertThat(code(res.body())).isEqualTo(0);

        HttpResponse<String> again = getAuth("/api/v1/resumes/" + resumeId);
        assertThat(again.statusCode()).isEqualTo(400);
    }

    private static byte[] multipartBody(
            String filename, String mime, byte[] fileBytes,
            String name, String type, String isDefault) {
        StringBuilder sb = new StringBuilder();
        appendPart(sb, "name", name);
        appendPart(sb, "type", type);
        appendPart(sb, "isDefault", isDefault);
        sb.append("--").append(BOUNDARY).append("\r\n");
        sb.append("Content-Disposition: form-data; name=\"file\"; filename=\"").append(filename).append("\"\r\n");
        sb.append("Content-Type: ").append(mime).append("\r\n\r\n");
        byte[] head = sb.toString().getBytes(StandardCharsets.UTF_8);
        byte[] tail = ("\r\n--" + BOUNDARY + "--\r\n").getBytes(StandardCharsets.UTF_8);
        byte[] all = new byte[head.length + fileBytes.length + tail.length];
        System.arraycopy(head, 0, all, 0, head.length);
        System.arraycopy(fileBytes, 0, all, head.length, fileBytes.length);
        System.arraycopy(tail, 0, all, head.length + fileBytes.length, tail.length);
        return all;
    }

    private static void appendPart(StringBuilder sb, String name, String value) {
        sb.append("--").append(BOUNDARY).append("\r\n");
        sb.append("Content-Disposition: form-data; name=\"").append(name).append("\"\r\n\r\n");
        sb.append(value).append("\r\n");
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

    private static String firstId(String body) {
        Matcher m = ID.matcher(body);
        assertThat(m.find()).as("body has id: %s", body).isTrue();
        return m.group(1);
    }
}
