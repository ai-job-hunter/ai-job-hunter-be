package com.aijobhunter.controller;

import com.aijobhunter.common.security.SecurityUtils;
import com.aijobhunter.domain.user.User;
import com.aijobhunter.dto.AnalyzeResumeRequest;
import com.aijobhunter.dto.ApiResponse;
import com.aijobhunter.dto.ResumeDTO;
import com.aijobhunter.dto.RewriteResumeRequest;
import com.aijobhunter.dto.UpdateResumeRequest;
import com.aijobhunter.service.ResumeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/resumes")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeService resumeService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ResumeDTO>> upload(
            @RequestPart("file") MultipartFile file,
            @RequestParam("name") String name,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "isDefault", required = false) Boolean isDefault) throws IOException {
        User user = SecurityUtils.requireCurrentUser();
        ResumeDTO dto = resumeService.upload(user.getId(), file, name, type, isDefault);
        return ResponseEntity.ok(ApiResponse.success("上传成功", dto));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> list(
            @RequestParam(value = "type", required = false) String type) {
        User user = SecurityUtils.requireCurrentUser();
        List<ResumeDTO> items = resumeService.list(user.getId(), type);
        Map<String, Object> data = new HashMap<>();
        data.put("items", items);
        data.put("page", 1);
        data.put("pageSize", items.size());
        data.put("total", items.size());
        data.put("totalPages", 1);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ResumeDTO>> detail(@PathVariable String id) {
        User user = SecurityUtils.requireCurrentUser();
        return ResponseEntity.ok(ApiResponse.success(resumeService.get(user.getId(), id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ResumeDTO>> update(
            @PathVariable String id,
            @RequestBody UpdateResumeRequest request) {
        User user = SecurityUtils.requireCurrentUser();
        return ResponseEntity.ok(ApiResponse.success("更新成功", resumeService.update(user.getId(), id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        User user = SecurityUtils.requireCurrentUser();
        resumeService.delete(user.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("删除成功", null));
    }

    /** 同步 AI 分析（失败降级）；返回完整简历含 analysis */
    @PostMapping("/{id}/analyze")
    public ResponseEntity<ApiResponse<ResumeDTO>> analyze(
            @PathVariable String id,
            @RequestBody(required = false) AnalyzeResumeRequest request) {
        User user = SecurityUtils.requireCurrentUser();
        return ResponseEntity.ok(ApiResponse.success(
                "分析完成", resumeService.analyze(user.getId(), id, request)));
    }

    /** 同步 AI 重写建议；结果写入 analysis.rewrite */
    @PostMapping("/{id}/rewrite")
    public ResponseEntity<ApiResponse<ResumeDTO>> rewrite(
            @PathVariable String id,
            @RequestBody(required = false) RewriteResumeRequest request) {
        User user = SecurityUtils.requireCurrentUser();
        return ResponseEntity.ok(ApiResponse.success(
                "重写完成", resumeService.rewrite(user.getId(), id, request)));
    }
}
