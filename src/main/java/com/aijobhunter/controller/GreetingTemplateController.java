package com.aijobhunter.controller;

import com.aijobhunter.common.security.SecurityUtils;
import com.aijobhunter.domain.user.User;
import com.aijobhunter.dto.ApiResponse;
import com.aijobhunter.service.GreetingTemplateService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/templates")
@RequiredArgsConstructor
public class GreetingTemplateController {

    private final GreetingTemplateService templateService;

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> list() {
        User user = SecurityUtils.requireCurrentUser();
        List<Map<String, Object>> items = templateService.list(user.getId());
        Map<String, Object> data = new HashMap<>();
        data.put("items", items);
        data.put("total", items.size());
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> create(@RequestBody TemplateRequest body) {
        User user = SecurityUtils.requireCurrentUser();
        Map<String, Object> data = templateService.create(
                user.getId(),
                body == null ? null : body.getName(),
                body == null ? null : body.getContent(),
                body == null ? null : body.getDescription(),
                body == null ? null : body.getIsDefault());
        return ResponseEntity.ok(ApiResponse.success("创建成功", data));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> update(
            @PathVariable String id,
            @RequestBody TemplateRequest body) {
        User user = SecurityUtils.requireCurrentUser();
        Map<String, Object> data = templateService.update(
                user.getId(), id,
                body == null ? null : body.getName(),
                body == null ? null : body.getContent(),
                body == null ? null : body.getDescription(),
                body == null ? null : body.getIsDefault(),
                body == null ? null : body.getIsActive());
        return ResponseEntity.ok(ApiResponse.success("已更新", data));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        User user = SecurityUtils.requireCurrentUser();
        templateService.delete(user.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("已删除", null));
    }

    @Data
    public static class TemplateRequest {
        private String name;
        private String content;
        private String description;
        private Boolean isDefault;
        private Boolean isActive;
    }
}
