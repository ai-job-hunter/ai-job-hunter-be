package com.aijobhunter.controller;

import com.aijobhunter.common.security.SecurityUtils;
import com.aijobhunter.domain.user.User;
import com.aijobhunter.dto.ApiResponse;
import com.aijobhunter.dto.CreateJobRequest;
import com.aijobhunter.dto.JobDTO;
import com.aijobhunter.service.JobService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;

    /** 粘贴 JD 创建岗位（可选立即 AI 分析） */
    @PostMapping
    public ResponseEntity<ApiResponse<JobDTO>> create(@RequestBody CreateJobRequest request) {
        User user = SecurityUtils.requireCurrentUser();
        JobDTO dto = jobService.create(user.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("创建成功", dto));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> list() {
        User user = SecurityUtils.requireCurrentUser();
        List<JobDTO> items = jobService.list(user.getId());
        Map<String, Object> data = new HashMap<>();
        data.put("items", items);
        data.put("page", 1);
        data.put("pageSize", items.size());
        data.put("total", items.size());
        data.put("totalPages", 1);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<JobDTO>> detail(@PathVariable String id) {
        User user = SecurityUtils.requireCurrentUser();
        return ResponseEntity.ok(ApiResponse.success(jobService.get(user.getId(), id)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        User user = SecurityUtils.requireCurrentUser();
        jobService.delete(user.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("删除成功", null));
    }

    @PostMapping("/{id}/analyze")
    public ResponseEntity<ApiResponse<JobDTO>> analyze(@PathVariable String id) {
        User user = SecurityUtils.requireCurrentUser();
        return ResponseEntity.ok(ApiResponse.success("分析完成", jobService.analyze(user.getId(), id)));
    }

    @GetMapping("/{id}/match")
    public ResponseEntity<ApiResponse<Map<String, Object>>> match(
            @PathVariable String id,
            @RequestParam("resumeId") String resumeId) {
        User user = SecurityUtils.requireCurrentUser();
        return ResponseEntity.ok(ApiResponse.success(jobService.match(user.getId(), id, resumeId)));
    }

    @PostMapping("/{id}/greet")
    public ResponseEntity<ApiResponse<Map<String, Object>>> greet(
            @PathVariable String id,
            @RequestBody(required = false) GreetBody body) {
        User user = SecurityUtils.requireCurrentUser();
        String resumeId = body != null ? body.getResumeId() : null;
        String style = body != null ? body.getStyle() : null;
        return ResponseEntity.ok(ApiResponse.success(
                "话术已生成", jobService.greet(user.getId(), id, resumeId, style, user)));
    }

    @Data
    public static class GreetBody {
        private String resumeId;
        private String style;
    }
}
