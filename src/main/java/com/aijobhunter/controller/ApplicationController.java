package com.aijobhunter.controller;

import com.aijobhunter.common.security.SecurityUtils;
import com.aijobhunter.domain.user.User;
import com.aijobhunter.dto.ApiResponse;
import com.aijobhunter.service.ApplicationService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> create(@RequestBody CreateApplicationRequest body) {
        User user = SecurityUtils.requireCurrentUser();
        Map<String, Object> data = applicationService.create(
                user.getId(),
                body == null ? null : body.getJobId(),
                body == null ? null : body.getResumeId(),
                body == null ? null : body.getApplyNote());
        return ResponseEntity.ok(ApiResponse.success("投递成功", data));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> list() {
        User user = SecurityUtils.requireCurrentUser();
        List<Map<String, Object>> items = applicationService.list(user.getId());
        Map<String, Object> data = new HashMap<>();
        data.put("items", items);
        data.put("page", 1);
        data.put("pageSize", items.size());
        data.put("total", items.size());
        data.put("totalPages", 1);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @GetMapping("/kanban")
    public ResponseEntity<ApiResponse<Map<String, Object>>> kanban() {
        User user = SecurityUtils.requireCurrentUser();
        return ResponseEntity.ok(ApiResponse.success(applicationService.kanban(user.getId())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> get(@PathVariable String id) {
        User user = SecurityUtils.requireCurrentUser();
        return ResponseEntity.ok(ApiResponse.success(applicationService.get(user.getId(), id)));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateStatus(
            @PathVariable String id,
            @RequestBody UpdateStatusRequest body) {
        User user = SecurityUtils.requireCurrentUser();
        return ResponseEntity.ok(ApiResponse.success(
                "状态已更新",
                applicationService.updateStatus(
                        user.getId(),
                        id,
                        body == null ? null : body.getStatus(),
                        body == null ? null : body.getNote())));
    }

    @PostMapping("/{id}/notes")
    public ResponseEntity<ApiResponse<Map<String, Object>>> addNote(
            @PathVariable String id,
            @RequestBody AddNoteRequest body) {
        User user = SecurityUtils.requireCurrentUser();
        return ResponseEntity.ok(ApiResponse.success(
                "备注已添加",
                applicationService.addNote(user.getId(), id, body == null ? null : body.getNote())));
    }

    @Data
    public static class CreateApplicationRequest {
        private String jobId;
        private String resumeId;
        private String applyNote;
    }

    @Data
    public static class UpdateStatusRequest {
        private String status;
        /** 可选：随状态变更写入时间线条目 */
        private String note;
    }

    @Data
    public static class AddNoteRequest {
        private String note;
    }
}
