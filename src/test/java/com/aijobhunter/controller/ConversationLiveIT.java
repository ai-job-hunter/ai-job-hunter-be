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
 * 沟通 + 投递 Live API：创建对话、录入消息、AI 推荐回复、投递看板。
 * 需本机后端已启动；推荐回复失败时后端会降级，用例不依赖真实 LLM。
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@EnabledIf("com.aijobhunter.controller.ConversationLiveIT#backendUp")
class ConversationLiveIT {

    private static final String BASE = System.getenv().getOrDefault("LIVE_API_BASE", "http://127.0.0.1:8080");
    private static final String USERNAME = "chat_" + UUID.randomUUID().toString().substring(0, 8);
    private static final String EMAIL = USERNAME + "@example.com";
    private static final String PASSWORD = "pass1234";
    private static final String BOUNDARY = "----AIJHChatBoundary" + UUID.randomUUID();

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    private static final Pattern CODE = Pattern.compile("\"code\"\\s*:\\s*(-?\\d+)");
    private static final Pattern TOKEN = Pattern.compile("\"token\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern ID = Pattern.compile("\"id\"\\s*:\\s*\"([0-9a-fA-F-]{36})\"");
    /** ApiResponse.data 顶层 id（避免嵌套 job/resume.id 抢先匹配） */
    private static final Pattern DATA_ID = Pattern.compile(
            "\"data\"\\s*:\\s*\\{[^}]*?\"id\"\\s*:\\s*\"([0-9a-fA-F-]{36})\"");

    private static String token;
    private static String jobId;
    private static String resumeId;
    private static String conversationId;
    private static String applicationId;

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
    static void registerAndSeed() throws Exception {
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
                "王五\nJava Spring Boot\n3年经验".getBytes(StandardCharsets.UTF_8),
                "沟通测简历",
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

        HttpResponse<String> jobRes = postAuth("/api/v1/jobs", """
                {
                  "title":"Java工程师",
                  "companyName":"沟通测试公司",
                  "city":"上海",
                  "rawDescription":"负责后端开发，熟悉 Spring Boot。",
                  "analyze":false
                }
                """);
        assertThat(jobRes.statusCode()).isEqualTo(200);
        jobId = firstId(jobRes.body());
        assertThat(jobId).isNotBlank();
        assertThat(resumeId).isNotBlank();
    }

    @Test
    @Order(1)
    @DisplayName("CHAT-001 创建对话并带首条 HR 消息")
    void createConversation() throws Exception {
        HttpResponse<String> res = postAuth("/api/v1/conversations", """
                {
                  "jobId":"%s",
                  "hrName":"张HR",
                  "firstHrMessage":"您好，方便聊聊期望薪资吗？"
                }
                """.formatted(jobId));
        assertThat(res.statusCode()).isEqualTo(200);
        assertThat(code(res.body())).isEqualTo(0);
        assertThat(res.body()).contains("张HR");
        conversationId = firstId(res.body());
        assertThat(conversationId).isNotBlank();
    }

    @Test
    @Order(2)
    @DisplayName("CHAT-002 对话列表包含新建项")
    void listConversations() throws Exception {
        HttpResponse<String> res = getAuth("/api/v1/conversations");
        assertThat(res.statusCode()).isEqualTo(200);
        assertThat(code(res.body())).isEqualTo(0);
        assertThat(res.body()).contains(conversationId);
        assertThat(res.body()).contains("张HR");
    }

    @Test
    @Order(3)
    @DisplayName("CHAT-003 对话详情含消息")
    void conversationDetail() throws Exception {
        HttpResponse<String> res = getAuth("/api/v1/conversations/" + conversationId);
        assertThat(res.statusCode()).isEqualTo(200);
        assertThat(code(res.body())).isEqualTo(0);
        assertThat(res.body()).contains("期望薪资");
        assertThat(res.body()).contains("INBOUND");
        assertThat(res.body()).contains("messages");
    }

    @Test
    @Order(4)
    @DisplayName("CHAT-004 AI 推荐回复")
    void suggestedReply() throws Exception {
        HttpResponse<String> res = getAuth("/api/v1/conversations/" + conversationId + "/suggested-reply");
        assertThat(res.statusCode()).isEqualTo(200);
        assertThat(code(res.body())).isEqualTo(0);
        assertThat(res.body()).contains("suggestions");
        assertThat(res.body()).contains("content");
    }

    @Test
    @Order(5)
    @DisplayName("CHAT-005 发送推荐回复")
    void sendSuggested() throws Exception {
        HttpResponse<String> res = postAuth(
                "/api/v1/conversations/" + conversationId + "/send-suggested-reply",
                """
                {"content":"期望 25k-30k，可谈。"}
                """);
        assertThat(res.statusCode()).isEqualTo(200);
        assertThat(code(res.body())).isEqualTo(0);
        assertThat(res.body()).contains("OUTBOUND");
        assertThat(res.body()).contains("25k-30k");
    }

    @Test
    @Order(6)
    @DisplayName("CHAT-006 手动录入 HR 消息")
    void addInbound() throws Exception {
        HttpResponse<String> res = postAuth(
                "/api/v1/conversations/" + conversationId + "/messages",
                """
                {"content":"好的，方便发一份简历吗？","direction":"INBOUND"}
                """);
        assertThat(res.statusCode()).isEqualTo(200);
        assertThat(code(res.body())).isEqualTo(0);
        assertThat(res.body()).contains("INBOUND");
    }

    @Test
    @Order(7)
    @DisplayName("APP-001 创建投递")
    void createApplication() throws Exception {
        HttpResponse<String> res = postAuth("/api/v1/applications", """
                {
                  "jobId":"%s",
                  "resumeId":"%s",
                  "applyNote":"LiveIT 投递"
                }
                """.formatted(jobId, resumeId));
        assertThat(res.statusCode()).isEqualTo(200);
        assertThat(code(res.body())).isEqualTo(0);
        assertThat(res.body()).contains("SUBMITTED");
        applicationId = firstId(res.body());
        assertThat(applicationId).isNotBlank();
    }

    @Test
    @Order(8)
    @DisplayName("APP-002 投递列表与看板")
    void listAndKanban() throws Exception {
        HttpResponse<String> list = getAuth("/api/v1/applications");
        assertThat(list.statusCode()).isEqualTo(200);
        assertThat(code(list.body())).isEqualTo(0);
        assertThat(list.body()).contains(applicationId);

        HttpResponse<String> kanban = getAuth("/api/v1/applications/kanban");
        assertThat(kanban.statusCode()).isEqualTo(200);
        assertThat(code(kanban.body())).isEqualTo(0);
        assertThat(kanban.body()).contains("columns");
        assertThat(kanban.body()).contains("SUBMITTED");
        assertThat(kanban.body()).contains(applicationId);
    }

    @Test
    @Order(9)
    @DisplayName("APP-003 更新投递状态")
    void updateStatus() throws Exception {
        HttpResponse<String> res = putAuth(
                "/api/v1/applications/" + applicationId + "/status",
                """
                {"status":"INTERVIEWING"}
                """);
        assertThat(res.statusCode()).isEqualTo(200);
        assertThat(code(res.body())).isEqualTo(0);
        assertThat(res.body()).contains("INTERVIEWING");
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

    private static HttpResponse<String> putAuth(String path, String json) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(BASE + path))
                .timeout(Duration.ofSeconds(30))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(json))
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
        Matcher data = DATA_ID.matcher(body);
        if (data.find()) {
            return data.group(1);
        }
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
