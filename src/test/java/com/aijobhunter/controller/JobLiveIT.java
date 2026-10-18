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
 * 岗位 Live API：粘贴 JD / 列表 / 详情 / 分析 / 匹配 / 打招呼 / 删除。
 * 需本机后端已启动；分析失败时后端会降级，用例不依赖真实 LLM。
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@EnabledIf("com.aijobhunter.controller.JobLiveIT#backendUp")
class JobLiveIT {

    private static final String BASE = System.getenv().getOrDefault("LIVE_API_BASE", "http://127.0.0.1:8080");
    private static final String USERNAME = "job_" + UUID.randomUUID().toString().substring(0, 8);
    private static final String EMAIL = USERNAME + "@example.com";
    private static final String PASSWORD = "pass1234";
    private static final String BOUNDARY = "----AIJHJobBoundary" + UUID.randomUUID();

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    private static final Pattern CODE = Pattern.compile("\"code\"\\s*:\\s*(-?\\d+)");
    private static final Pattern TOKEN = Pattern.compile("\"token\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern ID = Pattern.compile("\"id\"\\s*:\\s*\"([0-9a-fA-F-]{36})\"");

    private static String token;
    private static String jobId;
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
    static void registerLoginAndResume() throws Exception {
        postJson("/api/v1/auth/register", """
                {"username":"%s","email":"%s","password":"%s"}
                """.formatted(USERNAME, EMAIL, PASSWORD));
        HttpResponse<String> login = postJson("/api/v1/auth/login", """
                {"account":"%s","password":"%s"}
                """.formatted(EMAIL, PASSWORD));
        token = token(login.body());
        assertThat(token).isNotBlank();

        byte[] body = multipartBody(
                "resume.txt",
                "text/plain",
                "李四\nJava Spring Boot MySQL Redis\n5年经验".getBytes(StandardCharsets.UTF_8),
                "匹配用简历",
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
        resumeId = firstId(res.body());
        assertThat(resumeId).isNotBlank();
    }

    @Test
    @Order(1)
    @DisplayName("JOB-001 粘贴 JD 创建岗位")
    void createJob() throws Exception {
        HttpResponse<String> res = postAuth("/api/v1/jobs", """
                {
                  "title":"Java高级工程师",
                  "companyName":"测试科技",
                  "city":"北京",
                  "salaryText":"25k-40k",
                  "rawDescription":"岗位职责：负责 Java 后端开发。\\n要求：熟悉 Spring Boot、MySQL、Redis。",
                  "analyze":true
                }
                """);
        assertThat(res.statusCode()).isEqualTo(200);
        assertThat(code(res.body())).isEqualTo(0);
        assertThat(res.body()).contains("Java高级工程师");
        assertThat(res.body()).contains("测试科技");
        jobId = firstId(res.body());
        assertThat(jobId).isNotBlank();
    }

    @Test
    @Order(2)
    @DisplayName("JOB-002 岗位列表包含新建项")
    void listJobs() throws Exception {
        HttpResponse<String> res = getAuth("/api/v1/jobs");
        assertThat(res.statusCode()).isEqualTo(200);
        assertThat(code(res.body())).isEqualTo(0);
        assertThat(res.body()).contains(jobId);
        assertThat(res.body()).contains("Java高级工程师");
    }

    @Test
    @Order(3)
    @DisplayName("JOB-003 岗位详情")
    void jobDetail() throws Exception {
        HttpResponse<String> res = getAuth("/api/v1/jobs/" + jobId);
        assertThat(res.statusCode()).isEqualTo(200);
        assertThat(code(res.body())).isEqualTo(0);
        assertThat(res.body()).contains("Spring Boot");
        assertThat(res.body()).containsAnyOf("ANALYZED", "NEW", "GREETED");
    }

    @Test
    @Order(4)
    @DisplayName("JOB-004 重新分析 JD")
    void analyze() throws Exception {
        HttpResponse<String> res = postAuth("/api/v1/jobs/" + jobId + "/analyze", "{}");
        assertThat(res.statusCode()).isEqualTo(200);
        assertThat(code(res.body())).isEqualTo(0);
        assertThat(res.body()).contains("ANALYZED");
    }

    @Test
    @Order(5)
    @DisplayName("JOB-005 简历匹配")
    void match() throws Exception {
        HttpResponse<String> res = getAuth("/api/v1/jobs/" + jobId + "/match?resumeId=" + resumeId);
        assertThat(res.statusCode()).isEqualTo(200);
        assertThat(code(res.body())).isEqualTo(0);
        assertThat(res.body()).contains("overallScore");
        assertThat(res.body()).contains(resumeId);
    }

    @Test
    @Order(6)
    @DisplayName("JOB-006 生成打招呼话术")
    void greet() throws Exception {
        HttpResponse<String> res = postAuth("/api/v1/jobs/" + jobId + "/greet", """
                {"resumeId":"%s","style":"friendly"}
                """.formatted(resumeId));
        assertThat(res.statusCode()).isEqualTo(200);
        assertThat(code(res.body())).isEqualTo(0);
        assertThat(res.body()).contains("greetingText");
    }

    @Test
    @Order(7)
    @DisplayName("JOB-007 删除岗位")
    void deleteJob() throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(BASE + "/api/v1/jobs/" + jobId))
                .timeout(Duration.ofSeconds(10))
                .header("Authorization", "Bearer " + token)
                .DELETE()
                .build();
        HttpResponse<String> res = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
        assertThat(res.statusCode()).isEqualTo(200);
        assertThat(code(res.body())).isEqualTo(0);

        HttpResponse<String> detail = getAuth("/api/v1/jobs/" + jobId);
        assertThat(detail.statusCode()).isIn(400, 404);
    }

    private static HttpResponse<String> postJson(String path, String json) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(BASE + path))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        return CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
    }

    private static HttpResponse<String> postAuth(String path, String json) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(BASE + path))
                .timeout(Duration.ofSeconds(60))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        return CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
    }

    private static HttpResponse<String> getAuth(String path) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(BASE + path))
                .timeout(Duration.ofSeconds(30))
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();
        return CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
    }

    private static int code(String body) {
        Matcher m = CODE.matcher(body);
        return m.find() ? Integer.parseInt(m.group(1)) : -1;
    }

    private static String token(String body) {
        Matcher m = TOKEN.matcher(body);
        return m.find() ? m.group(1) : "";
    }

    private static String firstId(String body) {
        Matcher m = ID.matcher(body);
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
