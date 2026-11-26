package com.aijobhunter.controller;

import com.aijobhunter.common.security.SecurityUtils;
import com.aijobhunter.domain.user.User;
import com.aijobhunter.dto.ApiResponse;
import com.aijobhunter.service.PlatformService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/platforms")
@RequiredArgsConstructor
public class PlatformController {

    private final PlatformService platformService;

    @GetMapping("/connections")
    public ResponseEntity<ApiResponse<Map<String, Object>>> listConnections() {
        User user = SecurityUtils.requireCurrentUser();
        List<Map<String, Object>> items = platformService.listConnections(user.getId());
        Map<String, Object> data = new HashMap<>();
        data.put("items", items);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @PostMapping("/connections")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createConnection(
            @RequestBody CreateConnectionRequest body) {
        User user = SecurityUtils.requireCurrentUser();
        return ResponseEntity.ok(ApiResponse.success(
                "连接已创建",
                platformService.createConnection(user.getId(), body == null ? null : body.getPlatform())));
    }

    @GetMapping("/tasks")
    public ResponseEntity<ApiResponse<Map<String, Object>>> listTasks() {
        User user = SecurityUtils.requireCurrentUser();
        List<Map<String, Object>> items = platformService.listTasks(user.getId());
        Map<String, Object> data = new HashMap<>();
        data.put("items", items);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @PostMapping("/tasks")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createTask(@RequestBody CreateTaskRequest body) {
        User user = SecurityUtils.requireCurrentUser();
        return ResponseEntity.ok(ApiResponse.success(
                "任务已创建，待确认",
                platformService.createTask(
                        user.getId(),
                        body == null ? null : body.getConnectionId(),
                        body == null ? null : body.getAction(),
                        body == null ? null : body.getMode(),
                        body == null ? null : body.getRawDescription(),
                        body == null ? null : body.getTitle(),
                        body == null ? null : body.getCompanyName())));
    }

    @PostMapping("/tasks/{id}/confirm")
    public ResponseEntity<ApiResponse<Map<String, Object>>> confirmTask(@PathVariable String id) {
        User user = SecurityUtils.requireCurrentUser();
        return ResponseEntity.ok(ApiResponse.success(
                "任务已确认并完成（mock）",
                platformService.confirmTask(user.getId(), id)));
    }

    @Data
    public static class CreateConnectionRequest {
        private String platform;
    }

    @Data
    public static class CreateTaskRequest {
        private String connectionId;
        private String action;
        private String mode;
        /** SYNC_JOBS：本机粘贴的 JD 原文 */
        private String rawDescription;
        private String title;
        private String companyName;
    }
}
