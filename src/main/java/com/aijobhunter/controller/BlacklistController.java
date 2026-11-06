package com.aijobhunter.controller;

import com.aijobhunter.common.security.SecurityUtils;
import com.aijobhunter.domain.user.User;
import com.aijobhunter.dto.ApiResponse;
import com.aijobhunter.service.BlacklistService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/blacklist")
@RequiredArgsConstructor
public class BlacklistController {

    private final BlacklistService blacklistService;

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> list() {
        User user = SecurityUtils.requireCurrentUser();
        return ResponseEntity.ok(ApiResponse.success(blacklistService.list(user.getId())));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> add(@RequestBody AddRequest body) {
        User user = SecurityUtils.requireCurrentUser();
        LocalDateTime expires = null;
        if (body != null && body.getExpiresAt() != null && !body.getExpiresAt().isBlank()) {
            expires = LocalDateTime.parse(body.getExpiresAt().replace("Z", ""));
        }
        Map<String, Object> data = blacklistService.add(
                user.getId(),
                body == null ? null : body.getType(),
                body == null ? null : body.getValue(),
                body == null ? null : body.getReason(),
                expires);
        return ResponseEntity.ok(ApiResponse.success("已加入黑名单", data));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        User user = SecurityUtils.requireCurrentUser();
        blacklistService.delete(user.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("已删除", null));
    }

    @Data
    public static class AddRequest {
        private String type;
        private String value;
        private String reason;
        /** ISO-8601 本地或带 Z，如 2026-12-31T23:59:59 */
        private String expiresAt;
    }
}
