package com.aijobhunter.controller;

import com.aijobhunter.common.security.SecurityUtils;
import com.aijobhunter.domain.user.User;
import com.aijobhunter.dto.ApiResponse;
import com.aijobhunter.service.ConversationService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> create(@RequestBody CreateConversationRequest body) {
        User user = SecurityUtils.requireCurrentUser();
        Map<String, Object> data = conversationService.create(
                user.getId(),
                body == null ? null : body.getJobId(),
                body == null ? null : body.getHrName(),
                body == null ? null : body.getFirstHrMessage());
        return ResponseEntity.ok(ApiResponse.success("创建成功", data));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> list() {
        User user = SecurityUtils.requireCurrentUser();
        List<Map<String, Object>> items = conversationService.list(user.getId());
        Map<String, Object> data = new HashMap<>();
        data.put("items", items);
        data.put("page", 1);
        data.put("pageSize", items.size());
        data.put("total", items.size());
        data.put("totalPages", 1);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> detail(@PathVariable String id) {
        User user = SecurityUtils.requireCurrentUser();
        return ResponseEntity.ok(ApiResponse.success(conversationService.detail(user.getId(), id)));
    }

    @PostMapping("/{id}/messages")
    public ResponseEntity<ApiResponse<Map<String, Object>>> addMessage(
            @PathVariable String id,
            @RequestBody MessageRequest body) {
        User user = SecurityUtils.requireCurrentUser();
        Map<String, Object> msg = conversationService.addMessage(
                user.getId(), id,
                body == null ? null : body.getContent(),
                body == null ? null : body.getDirection());
        return ResponseEntity.ok(ApiResponse.success("发送成功", msg));
    }

    @GetMapping("/{id}/suggested-reply")
    public ResponseEntity<ApiResponse<Map<String, Object>>> suggestedReply(@PathVariable String id) {
        User user = SecurityUtils.requireCurrentUser();
        return ResponseEntity.ok(ApiResponse.success(
                conversationService.suggestedReply(user.getId(), user, id)));
    }

    @PostMapping("/{id}/send-suggested-reply")
    public ResponseEntity<ApiResponse<Map<String, Object>>> sendSuggested(
            @PathVariable String id,
            @RequestBody SendSuggestedRequest body) {
        User user = SecurityUtils.requireCurrentUser();
        return ResponseEntity.ok(ApiResponse.success(
                "已发送", conversationService.sendSuggested(user.getId(), id, body == null ? null : body.getContent())));
    }

    @Data
    public static class CreateConversationRequest {
        private String jobId;
        private String hrName;
        private String firstHrMessage;
    }

    @Data
    public static class MessageRequest {
        private String content;
        /** INBOUND=录入 HR 消息；OUTBOUND=我发出（默认） */
        private String direction;
    }

    @Data
    public static class SendSuggestedRequest {
        private Integer suggestionIndex;
        private String content;
    }
}
