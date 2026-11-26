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

    private static final String BOUNDARY = "----PlatLiveBoundary7MA4YWxk";

    private static String token;
    private static String connectionId;
    private static String taskId;
    private static String importedJobId;
    private static String resumeId;
    private static String conversationId;

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
        importedJobId = extractField(res.body(), "jobId");
        assertThat(importedJobId).isNotBlank();

        HttpResponse<String> jobs = getAuth("/api/v1/jobs");
        assertThat(jobs.statusCode()).isEqualTo(200);
        assertThat(jobs.body()).contains("IMPORT");
        assertThat(jobs.body()).contains("导入测试岗");
    }

    @Test
    @Order(5)
    @DisplayName("PLAT-005 创建打招呼草稿任务（须 draftText）")
    void createDraftGreetTask() throws Exception {
        HttpResponse<String> missing = postAuth("/api/v1/platforms/tasks", """
                {
                  "connectionId":"%s",
                  "action":"DRAFT_GREET",
                  "mode":"SEMI_AUTO"
                }
                """.formatted(connectionId));
        assertThat(missing.statusCode()).isEqualTo(400);

        HttpResponse<String> res = postAuth("/api/v1/platforms/tasks", """
                {
                  "connectionId":"%s",
                  "action":"DRAFT_GREET",
                  "mode":"SEMI_AUTO",
                  "draftText":"您好，我对贵司 Java 岗位很感兴趣。"
                }
                """.formatted(connectionId));
        assertThat(res.statusCode()).isEqualTo(200);
        assertThat(code(res.body())).isEqualTo(0);
        assertThat(res.body()).contains("NEED_CONFIRM");
        taskId = firstId(res.body());
        assertThat(taskId).isNotBlank();
    }

    @Test
    @Order(6)
    @DisplayName("PLAT-006 确认草稿 → clipboardText")
    void confirmDraftGreet() throws Exception {
        HttpResponse<String> res = postAuth("/api/v1/platforms/tasks/" + taskId + "/confirm", "{}");
        assertThat(res.statusCode()).isEqualTo(200);
        assertThat(code(res.body())).isEqualTo(0);
        assertThat(res.body()).contains("DONE");
        assertThat(res.body()).contains("clipboardText");
        assertThat(res.body()).contains("我对贵司 Java 岗位很感兴趣");
    }

    @Test
    @Order(7)
    @DisplayName("PLAT-007 上传简历供标记投递")
    void uploadResumeForMarkApplied() throws Exception {
        byte[] body = multipartBody(
                "plat-resume.txt",
                "text/plain",
                "李四\nJava\n3年".getBytes(StandardCharsets.UTF_8),
                "平台测简历",
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
        resumeId = firstId(res.body());
        assertThat(resumeId).isNotBlank();
    }

    @Test
    @Order(8)
    @DisplayName("PLAT-008 MARK_APPLIED 创建任务须 jobId+resumeId")
    void createMarkAppliedTask() throws Exception {
        HttpResponse<String> missing = postAuth("/api/v1/platforms/tasks", """
                {
                  "connectionId":"%s",
                  "action":"MARK_APPLIED",
                  "mode":"SEMI_AUTO"
                }
                """.formatted(connectionId));
        assertThat(missing.statusCode()).isEqualTo(400);

        HttpResponse<String> res = postAuth("/api/v1/platforms/tasks", """
                {
                  "connectionId":"%s",
                  "action":"MARK_APPLIED",
                  "mode":"SEMI_AUTO",
                  "jobId":"%s",
                  "resumeId":"%s",
                  "applyNote":"平台页标记"
                }
                """.formatted(connectionId, importedJobId, resumeId));
        assertThat(res.statusCode()).isEqualTo(200);
        assertThat(code(res.body())).isEqualTo(0);
        assertThat(res.body()).contains("NEED_CONFIRM");
        taskId = firstId(res.body());
    }

    @Test
    @Order(9)
    @DisplayName("PLAT-009 确认 MARK_APPLIED → 投递看板")
    void confirmMarkApplied() throws Exception {
        HttpResponse<String> res = postAuth("/api/v1/platforms/tasks/" + taskId + "/confirm", "{}");
        assertThat(res.statusCode()).isEqualTo(200);
        assertThat(code(res.body())).isEqualTo(0);
        assertThat(res.body()).contains("DONE");
        assertThat(res.body()).contains("applicationId");
        assertThat(res.body()).contains("已记入投递看板");

        HttpResponse<String> apps = getAuth("/api/v1/applications");
        assertThat(apps.statusCode()).isEqualTo(200);
        assertThat(apps.body()).contains(importedJobId);
        assertThat(apps.body()).contains("SUBMITTED");
    }

    @Test
    @Order(10)
    @DisplayName("PLAT-010 IMPORT_CHAT 创建任务须 chatText")
    void createImportChatTask() throws Exception {
        HttpResponse<String> missing = postAuth("/api/v1/platforms/tasks", """
                {
                  "connectionId":"%s",
                  "action":"IMPORT_CHAT",
                  "mode":"SEMI_AUTO"
                }
                """.formatted(connectionId));
        assertThat(missing.statusCode()).isEqualTo(400);

        HttpResponse<String> res = postAuth("/api/v1/platforms/tasks", """
                {
                  "connectionId":"%s",
                  "action":"IMPORT_CHAT",
                  "mode":"SEMI_AUTO",
                  "hrName":"王HR",
                  "jobId":"%s",
                  "chatText":"HR: 您好，方便聊聊吗？\\n我: 方便，请问技术栈？\\n还在看人吗"
                }
                """.formatted(connectionId, importedJobId));
        assertThat(res.statusCode()).isEqualTo(200);
        assertThat(code(res.body())).isEqualTo(0);
        assertThat(res.body()).contains("NEED_CONFIRM");
        taskId = firstId(res.body());
    }

    @Test
    @Order(11)
    @DisplayName("PLAT-011 确认 IMPORT_CHAT → 沟通列表")
    void confirmImportChat() throws Exception {
        HttpResponse<String> res = postAuth("/api/v1/platforms/tasks/" + taskId + "/confirm", "{}");
        assertThat(res.statusCode()).isEqualTo(200);
        assertThat(code(res.body())).isEqualTo(0);
        assertThat(res.body()).contains("DONE");
        assertThat(res.body()).contains("conversationId");
        assertThat(res.body()).contains("importedMessages");
        conversationId = extractField(res.body(), "conversationId");
        assertThat(conversationId).isNotBlank();

        HttpResponse<String> convs = getAuth("/api/v1/conversations");
        assertThat(convs.statusCode()).isEqualTo(200);
        assertThat(convs.body()).contains("王HR");
    }

    @Test
    @Order(12)
    @DisplayName("PLAT-012 SUGGEST_REPLY 生成待确认话术")
    void createSuggestReplyTask() throws Exception {
        HttpResponse<String> missing = postAuth("/api/v1/platforms/tasks", """
                {
                  "connectionId":"%s",
                  "action":"SUGGEST_REPLY",
                  "mode":"SEMI_AUTO"
                }
                """.formatted(connectionId));
        assertThat(missing.statusCode()).isEqualTo(400);

        HttpResponse<String> res = postAuth("/api/v1/platforms/tasks", """
                {
                  "connectionId":"%s",
                  "action":"SUGGEST_REPLY",
                  "mode":"SEMI_AUTO",
                  "conversationId":"%s"
                }
                """.formatted(connectionId, conversationId));
        assertThat(res.statusCode()).isEqualTo(200);
        assertThat(code(res.body())).isEqualTo(0);
        assertThat(res.body()).contains("NEED_CONFIRM");
        assertThat(res.body()).contains("draftText");
        taskId = firstId(res.body());
    }

    @Test
    @Order(13)
    @DisplayName("PLAT-013 确认 SUGGEST_REPLY → 记入会话 + clipboardText")
    void confirmSuggestReply() throws Exception {
        HttpResponse<String> res = postAuth("/api/v1/platforms/tasks/" + taskId + "/confirm", "{}");
        assertThat(res.statusCode()).isEqualTo(200);
        assertThat(code(res.body())).isEqualTo(0);
        assertThat(res.body()).contains("DONE");
        assertThat(res.body()).contains("clipboardText");
        assertThat(res.body()).contains("messageId");
        assertThat(res.body()).contains("已记入会话");

        HttpResponse<String> detail = getAuth("/api/v1/conversations/" + conversationId);
        assertThat(detail.statusCode()).isEqualTo(200);
        assertThat(detail.body()).contains("OUTBOUND");
    }

    private static String extractField(String body, String field) {
        Matcher m = Pattern.compile("\"" + field + "\"\\s*:\\s*\"([^\"]+)\"").matcher(body);
        assertThat(m.find()).as("body has %s: %s", field, body).isTrue();
        return m.group(1);
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
