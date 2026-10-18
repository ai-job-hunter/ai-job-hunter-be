package com.aijobhunter.controller;

import com.aijobhunter.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.web.server.LocalServerPort;

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
 * CI 全链路 API（Testcontainers 起真实 PG + Redis + 本进程 Spring）。
 * <p>
 * 覆盖：注册 → 登录 → /users/me → 简历上传 → 粘贴 JD → 列表，保证前后端契约在 CI 可验证。
 * 需 {@code RUN_TESTCONTAINERS=true}（GitHub Actions smoke job 已配置）。
 */
@EnabledIfEnvironmentVariable(named = "RUN_TESTCONTAINERS", matches = "true")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ApiFlowIT extends AbstractIntegrationTest {

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private static final Pattern CODE = Pattern.compile("\"code\"\\s*:\\s*(-?\\d+)");
    private static final Pattern TOKEN = Pattern.compile("\"token\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern ID = Pattern.compile("\"id\"\\s*:\\s*\"([0-9a-fA-F-]{36})\"");

    private static final String USERNAME = "flow_" + UUID.randomUUID().toString().substring(0, 8);
    private static final String EMAIL = USERNAME + "@example.com";
    private static final String PASSWORD = "pass1234";
    private static final String BOUNDARY = "----FlowBoundary" + UUID.randomUUID();

    @LocalServerPort
    private int port;

    private static String token;
    private static String resumeId;
    private static String jobId;

    private String base() {
        return "http://127.0.0.1:" + port;
    }

    @Test
    @Order(1)
    @DisplayName("FLOW-001 注册 + 登录拿到 token")
    void registerAndLogin() throws Exception {
        HttpResponse<String> reg = postJson("/api/v1/auth/register", """
                {"username":"%s","email":"%s","password":"%s"}
                """.formatted(USERNAME, EMAIL, PASSWORD));
        assertThat(reg.statusCode()).isEqualTo(200);
        assertThat(code(reg.body())).isEqualTo(0);
        assertThat(reg.body()).doesNotContain("\"token\"");

        HttpResponse<String> login = postJson("/api/v1/auth/login", """
                {"account":"%s","password":"%s"}
                """.formatted(EMAIL, PASSWORD));
        assertThat(login.statusCode()).isEqualTo(200);
        assertThat(code(login.body())).isEqualTo(0);
        token = token(login.body());
        assertThat(token).isNotBlank();
    }

    @Test
    @Order(2)
    @DisplayName("FLOW-002 GET /users/me 需登录；无 token → 401")
    void usersMe() throws Exception {
        HttpResponse<String> anon = CLIENT.send(
                HttpRequest.newBuilder(URI.create(base() + "/api/v1/users/me"))
                        .timeout(Duration.ofSeconds(10)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(anon.statusCode()).isEqualTo(401);

        HttpResponse<String> me = getAuth("/api/v1/users/me");
        assertThat(me.statusCode()).isEqualTo(200);
        assertThat(code(me.body())).isEqualTo(0);
        assertThat(me.body()).contains(USERNAME);
        assertThat(me.body()).contains(EMAIL);
    }

    @Test
    @Order(3)
    @DisplayName("FLOW-003 上传 txt 简历")
    void uploadResume() throws Exception {
        byte[] body = multipartBody(
                "cv.txt",
                "text/plain",
                "王五\nJava Spring Boot\n3年经验".getBytes(StandardCharsets.UTF_8),
                "流程简历",
                "TECHNICAL",
                "true");
        HttpRequest req = HttpRequest.newBuilder(URI.create(base() + "/api/v1/resumes/upload"))
                .timeout(Duration.ofSeconds(30))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "multipart/form-data; boundary=" + BOUNDARY)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();
        HttpResponse<String> res = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
        assertThat(res.statusCode()).isEqualTo(200);
        assertThat(code(res.body())).isEqualTo(0);
        assertThat(res.body()).contains("PARSED");
        resumeId = firstId(res.body());
        assertThat(resumeId).isNotBlank();
    }

    @Test
    @Order(4)
    @DisplayName("FLOW-004 粘贴 JD 创建岗位并出现在列表")
    void createAndListJob() throws Exception {
        HttpResponse<String> created = postAuth("/api/v1/jobs", """
                {
                  "title":"Java工程师",
                  "companyName":"流程科技",
                  "city":"上海",
                  "rawDescription":"职责：Java 后端。要求：Spring Boot、MySQL。",
                  "analyze":true
                }
                """);
        assertThat(created.statusCode()).isEqualTo(200);
        assertThat(code(created.body())).isEqualTo(0);
        assertThat(created.body()).contains("Java工程师");
        jobId = firstId(created.body());
        assertThat(jobId).isNotBlank();

        HttpResponse<String> list = getAuth("/api/v1/jobs");
        assertThat(list.statusCode()).isEqualTo(200);
        assertThat(list.body()).contains(jobId);
        assertThat(list.body()).contains("流程科技");
    }

    @Test
    @Order(5)
    @DisplayName("FLOW-005 匹配 + 打招呼（LLM 可降级）")
    void matchAndGreet() throws Exception {
        HttpResponse<String> match = getAuth("/api/v1/jobs/" + jobId + "/match?resumeId=" + resumeId);
        assertThat(match.statusCode()).isEqualTo(200);
        assertThat(code(match.body())).isEqualTo(0);
        assertThat(match.body()).contains("overallScore");

        HttpResponse<String> greet = postAuth("/api/v1/jobs/" + jobId + "/greet", """
                {"resumeId":"%s","style":"friendly"}
                """.formatted(resumeId));
        assertThat(greet.statusCode()).isEqualTo(200);
        assertThat(code(greet.body())).isEqualTo(0);
        assertThat(greet.body()).contains("greetingText");
    }

    private HttpResponse<String> getAuth(String path) throws Exception {
        return CLIENT.send(HttpRequest.newBuilder(URI.create(base() + path))
                .timeout(Duration.ofSeconds(60))
                .header("Authorization", "Bearer " + token)
                .GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> postAuth(String path, String json) throws Exception {
        return CLIENT.send(HttpRequest.newBuilder(URI.create(base() + path))
                .timeout(Duration.ofSeconds(90))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> postJson(String path, String json) throws Exception {
        return CLIENT.send(HttpRequest.newBuilder(URI.create(base() + path))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build(), HttpResponse.BodyHandlers.ofString());
    }

    private static int code(String body) {
        Matcher m = CODE.matcher(body);
        assertThat(m.find()).as("code in %s", body).isTrue();
        return Integer.parseInt(m.group(1));
    }

    private static String token(String body) {
        Matcher m = TOKEN.matcher(body);
        assertThat(m.find()).as("token in %s", body).isTrue();
        return m.group(1);
    }

    private static String firstId(String body) {
        Matcher m = ID.matcher(body);
        assertThat(m.find()).as("id in %s", body).isTrue();
        return m.group(1);
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
