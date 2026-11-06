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
 * Sprint5：统计 / 模板 / 黑名单 Live API。
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@EnabledIf("com.aijobhunter.controller.StatsLiveIT#backendUp")
class StatsLiveIT {

    private static final String BASE = System.getenv().getOrDefault("LIVE_API_BASE", "http://127.0.0.1:8080");
    private static final String USERNAME = "stat_" + UUID.randomUUID().toString().substring(0, 8);
    private static final String EMAIL = USERNAME + "@example.com";
    private static final String PASSWORD = "pass1234";
    private static final String BOUNDARY = "----AIJHStatBoundary" + UUID.randomUUID();

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    private static final Pattern CODE = Pattern.compile("\"code\"\\s*:\\s*(-?\\d+)");
    private static final Pattern TOKEN = Pattern.compile("\"token\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern DATA_ID = Pattern.compile(
            "\"data\"\\s*:\\s*\\{[^}]*?\"id\"\\s*:\\s*\"([0-9a-fA-F-]{36})\"");

    private static String token;
    private static String templateId;
    private static String blacklistId;

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
    static void seed() throws Exception {
        postJson("/api/v1/auth/register", """
                {"username":"%s","email":"%s","password":"%s"}
                """.formatted(USERNAME, EMAIL, PASSWORD));
        HttpResponse<String> login = postJson("/api/v1/auth/login", """
                {"account":"%s","password":"%s"}
                """.formatted(EMAIL, PASSWORD));
        token = token(login.body());
        assertThat(token).isNotBlank();

        byte[] body = multipartBody(
                "r.txt", "text/plain",
                "统计测\nJava".getBytes(StandardCharsets.UTF_8),
                "统计简历", "TECHNICAL", "true");
        HttpRequest req = HttpRequest.newBuilder(URI.create(BASE + "/api/v1/resumes/upload"))
                .timeout(Duration.ofSeconds(20))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "multipart/form-data; boundary=" + BOUNDARY)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();
        assertThat(CLIENT.send(req, HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(200);

        HttpResponse<String> job = postAuth("/api/v1/jobs", """
                {"title":"统计岗","companyName":"统计公司","rawDescription":"Java 后端","analyze":false}
                """);
        assertThat(job.statusCode()).isEqualTo(200);
    }

    @Test
    @Order(1)
    @DisplayName("STAT-001 今日概览")
    void today() throws Exception {
        HttpResponse<String> res = getAuth("/api/v1/stats/today");
        assertThat(res.statusCode()).isEqualTo(200);
        assertThat(code(res.body())).isEqualTo(0);
        assertThat(res.body()).contains("jobsDiscovered");
        assertThat(res.body()).contains("replyRate");
    }

    @Test
    @Order(2)
    @DisplayName("STAT-002 漏斗")
    void funnel() throws Exception {
        HttpResponse<String> res = getAuth("/api/v1/stats/funnel");
        assertThat(res.statusCode()).isEqualTo(200);
        assertThat(code(res.body())).isEqualTo(0);
        assertThat(res.body()).contains("funnel");
        assertThat(res.body()).contains("发现岗位");
    }

    @Test
    @Order(3)
    @DisplayName("STAT-003 趋势")
    void trend() throws Exception {
        HttpResponse<String> res = getAuth("/api/v1/stats/trend?period=week");
        assertThat(res.statusCode()).isEqualTo(200);
        assertThat(code(res.body())).isEqualTo(0);
        assertThat(res.body()).contains("dataPoints");
        assertThat(res.body()).contains("summary");
    }

    @Test
    @Order(4)
    @DisplayName("STAT-004 周报（可降级）")
    void weeklyReport() throws Exception {
        HttpResponse<String> res = postAuth("/api/v1/stats/weekly-report", "{}");
        assertThat(res.statusCode()).isEqualTo(200);
        assertThat(code(res.body())).isEqualTo(0);
        assertThat(res.body()).contains("content");
        assertThat(res.body()).contains("weekStartDate");
    }

    @Test
    @Order(5)
    @DisplayName("TPL-001 打招呼模板 CRUD")
    void templates() throws Exception {
        HttpResponse<String> created = postAuth("/api/v1/templates", """
                {"name":"友好版","content":"您好，我对贵司很感兴趣","isDefault":true}
                """);
        assertThat(created.statusCode()).isEqualTo(200);
        assertThat(code(created.body())).isEqualTo(0);
        templateId = dataId(created.body());
        assertThat(templateId).isNotBlank();

        HttpResponse<String> list = getAuth("/api/v1/templates");
        assertThat(list.body()).contains(templateId);
        assertThat(list.body()).contains("友好版");

        HttpResponse<String> del = deleteAuth("/api/v1/templates/" + templateId);
        assertThat(del.statusCode()).isEqualTo(200);
        assertThat(code(del.body())).isEqualTo(0);
    }

    @Test
    @Order(6)
    @DisplayName("BL-001 黑名单 CRUD")
    void blacklist() throws Exception {
        HttpResponse<String> created = postAuth("/api/v1/blacklist", """
                {"type":"COMPANY","value":"不感兴趣公司","reason":"加班多"}
                """);
        assertThat(created.statusCode()).isEqualTo(200);
        assertThat(code(created.body())).isEqualTo(0);
        blacklistId = dataId(created.body());
        assertThat(blacklistId).isNotBlank();

        HttpResponse<String> list = getAuth("/api/v1/blacklist");
        assertThat(list.body()).contains(blacklistId);
        assertThat(list.body()).contains("不感兴趣公司");

        HttpResponse<String> del = deleteAuth("/api/v1/blacklist/" + blacklistId);
        assertThat(del.statusCode()).isEqualTo(200);
        assertThat(code(del.body())).isEqualTo(0);
    }

    private static HttpResponse<String> postJson(String path, String json) throws Exception {
        return CLIENT.send(HttpRequest.newBuilder(URI.create(BASE + path))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build(), HttpResponse.BodyHandlers.ofString());
    }

    private static HttpResponse<String> postAuth(String path, String json) throws Exception {
        return CLIENT.send(HttpRequest.newBuilder(URI.create(BASE + path))
                .timeout(Duration.ofSeconds(90))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build(), HttpResponse.BodyHandlers.ofString());
    }

    private static HttpResponse<String> getAuth(String path) throws Exception {
        return CLIENT.send(HttpRequest.newBuilder(URI.create(BASE + path))
                .timeout(Duration.ofSeconds(30))
                .header("Authorization", "Bearer " + token)
                .GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    private static HttpResponse<String> deleteAuth(String path) throws Exception {
        return CLIENT.send(HttpRequest.newBuilder(URI.create(BASE + path))
                .timeout(Duration.ofSeconds(15))
                .header("Authorization", "Bearer " + token)
                .DELETE().build(), HttpResponse.BodyHandlers.ofString());
    }

    private static int code(String body) {
        Matcher m = CODE.matcher(body);
        return m.find() ? Integer.parseInt(m.group(1)) : -1;
    }

    private static String token(String body) {
        Matcher m = TOKEN.matcher(body);
        return m.find() ? m.group(1) : "";
    }

    private static String dataId(String body) {
        Matcher m = DATA_ID.matcher(body);
        return m.find() ? m.group(1) : "";
    }

    private static byte[] multipartBody(String filename, String contentType, byte[] fileBytes,
                                        String name, String type, String isDefault) {
        String mid = "\r\n--" + BOUNDARY + "\r\n";
        StringBuilder sb = new StringBuilder();
        sb.append("--").append(BOUNDARY).append("\r\n");
        sb.append("Content-Disposition: form-data; name=\"file\"; filename=\"").append(filename).append("\"\r\n");
        sb.append("Content-Type: ").append(contentType).append("\r\n\r\n");
        byte[] head = sb.toString().getBytes(StandardCharsets.UTF_8);
        String fields = mid
                + "Content-Disposition: form-data; name=\"name\"\r\n\r\n" + name
                + mid
                + "Content-Disposition: form-data; name=\"type\"\r\n\r\n" + type
                + mid
                + "Content-Disposition: form-data; name=\"isDefault\"\r\n\r\n" + isDefault
                + "\r\n--" + BOUNDARY + "--\r\n";
        byte[] tail = fields.getBytes(StandardCharsets.UTF_8);
        byte[] all = new byte[head.length + fileBytes.length + tail.length];
        System.arraycopy(head, 0, all, 0, head.length);
        System.arraycopy(fileBytes, 0, all, head.length, fileBytes.length);
        System.arraycopy(tail, 0, all, head.length + fileBytes.length, tail.length);
        return all;
    }
}
