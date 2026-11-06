package com.aijobhunter.controller;

import com.aijobhunter.common.security.SecurityUtils;
import com.aijobhunter.domain.user.User;
import com.aijobhunter.dto.ApiResponse;
import com.aijobhunter.service.StatsService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    @GetMapping("/today")
    public ResponseEntity<ApiResponse<Map<String, Object>>> today() {
        User user = SecurityUtils.requireCurrentUser();
        return ResponseEntity.ok(ApiResponse.success(statsService.today(user.getId())));
    }

    @GetMapping("/trend")
    public ResponseEntity<ApiResponse<Map<String, Object>>> trend(
            @RequestParam(defaultValue = "week") String period) {
        User user = SecurityUtils.requireCurrentUser();
        return ResponseEntity.ok(ApiResponse.success(statsService.trend(user.getId(), period)));
    }

    @GetMapping("/funnel")
    public ResponseEntity<ApiResponse<Map<String, Object>>> funnel() {
        User user = SecurityUtils.requireCurrentUser();
        return ResponseEntity.ok(ApiResponse.success(statsService.funnel(user.getId())));
    }

    @PostMapping("/weekly-report")
    public ResponseEntity<ApiResponse<Map<String, Object>>> weeklyReport(
            @RequestBody(required = false) WeeklyReportRequest body) {
        User user = SecurityUtils.requireCurrentUser();
        LocalDate weekStart = null;
        if (body != null && body.getWeekStartDate() != null && !body.getWeekStartDate().isBlank()) {
            weekStart = LocalDate.parse(body.getWeekStartDate());
        }
        return ResponseEntity.ok(ApiResponse.success(
                "周报已生成", statsService.weeklyReport(user.getId(), weekStart)));
    }

    @Data
    public static class WeeklyReportRequest {
        private String weekStartDate;
    }
}
