package com.aijobhunter.service;

import com.aijobhunter.domain.application.Application;
import com.aijobhunter.domain.application.ApplicationRepository;
import com.aijobhunter.domain.job.Job;
import com.aijobhunter.domain.job.JobRepository;
import com.aijobhunter.domain.resume.Resume;
import com.aijobhunter.domain.resume.ResumeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final JobRepository jobRepository;
    private final ResumeRepository resumeRepository;

    @Transactional
    public Map<String, Object> create(String userId, String jobId, String resumeId, String note) {
        if (jobId == null || jobId.isBlank()) {
            throw new IllegalArgumentException("请选择岗位");
        }
        if (resumeId == null || resumeId.isBlank()) {
            throw new IllegalArgumentException("请选择简历");
        }
        Job job = jobRepository.findByIdAndUserId(jobId, userId)
                .orElseThrow(() -> new IllegalArgumentException("岗位不存在"));
        Resume resume = resumeRepository.findByIdAndUserId(resumeId, userId)
                .orElseThrow(() -> new IllegalArgumentException("简历不存在"));
        if (applicationRepository.existsByUserIdAndJobId(userId, jobId)) {
            throw new IllegalStateException("该岗位已投递过");
        }

        Integer matchScore = null;
        if (job.getMatchScores() != null && job.getMatchScores().get(resumeId) instanceof Map<?, ?> m
                && m.get("overallScore") instanceof Number n) {
            matchScore = n.intValue();
        }

        Application app = Application.builder()
                .userId(userId)
                .jobId(job.getId())
                .resumeId(resume.getId())
                .applyNote(note == null || note.isBlank() ? null : note.trim())
                .matchScore(matchScore)
                .status(Application.ApplicationStatus.SUBMITTED)
                .build();
        app = applicationRepository.save(app);

        if (job.getStatus() == Job.JobStatus.NEW
                || job.getStatus() == Job.JobStatus.ANALYZED
                || job.getStatus() == Job.JobStatus.GREETED) {
            job.setStatus(Job.JobStatus.APPLIED);
            jobRepository.save(job);
        }

        return toDto(app, job, resume);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> list(String userId) {
        return applicationRepository.findByUserIdOrderByAppliedAtDesc(userId).stream()
                .map(this::toDtoResolved)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> kanban(String userId) {
        List<Application> all = applicationRepository.findByUserIdOrderByAppliedAtDesc(userId);
        Map<String, List<Map<String, Object>>> buckets = new LinkedHashMap<>();
        for (Application.ApplicationStatus s : Application.ApplicationStatus.values()) {
            buckets.put(s.name(), new ArrayList<>());
        }
        for (Application app : all) {
            String key = app.getStatus() == null ? Application.ApplicationStatus.SUBMITTED.name() : app.getStatus().name();
            buckets.computeIfAbsent(key, k -> new ArrayList<>()).add(toDtoResolved(app));
        }

        List<Map<String, Object>> columns = new ArrayList<>();
        Map<String, String> titles = Map.of(
                "SUBMITTED", "已投递",
                "VIEWED", "已查看",
                "REPLIED", "有回复",
                "INTERVIEWING", "面试中",
                "OFFERED", "Offer",
                "REJECTED", "已拒绝",
                "WITHDRAWN", "已撤回");
        for (Map.Entry<String, List<Map<String, Object>>> e : buckets.entrySet()) {
            Map<String, Object> col = new HashMap<>();
            col.put("status", e.getKey());
            col.put("title", titles.getOrDefault(e.getKey(), e.getKey()));
            col.put("count", e.getValue().size());
            col.put("items", e.getValue());
            columns.add(col);
        }
        Map<String, Object> data = new HashMap<>();
        data.put("columns", columns);
        return data;
    }

    @Transactional
    public Map<String, Object> updateStatus(String userId, String id, String status) {
        Application app = applicationRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("投递记录不存在"));
        try {
            app.setStatus(Application.ApplicationStatus.valueOf(status.trim().toUpperCase(Locale.ROOT)));
        } catch (Exception e) {
            throw new IllegalArgumentException("不支持的状态: " + status);
        }
        app = applicationRepository.save(app);
        return toDtoResolved(app);
    }

    private Map<String, Object> toDtoResolved(Application app) {
        Job job = app.getJobId() == null ? null
                : jobRepository.findByIdAndUserId(app.getJobId(), app.getUserId()).orElse(null);
        Resume resume = app.getResumeId() == null ? null
                : resumeRepository.findByIdAndUserId(app.getResumeId(), app.getUserId()).orElse(null);
        return toDto(app, job, resume);
    }

    private Map<String, Object> toDto(Application app, Job job, Resume resume) {
        // LinkedHashMap：保证 JSON 里顶层 id 先于嵌套 job/resume.id，便于客户端/测试解析
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", app.getId());
        data.put("status", app.getStatus() == null ? null : app.getStatus().name());
        data.put("matchScore", app.getMatchScore());
        data.put("appliedAt", app.getAppliedAt());
        data.put("applyNote", app.getApplyNote());
        data.put("conversationId", app.getConversationId());
        if (job != null) {
            Map<String, Object> j = new LinkedHashMap<>();
            j.put("id", job.getId());
            j.put("title", job.getTitle());
            j.put("companyName", job.getCompanyName());
            j.put("city", job.getCity());
            data.put("job", j);
        } else {
            data.put("job", null);
        }
        if (resume != null) {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("id", resume.getId());
            r.put("name", resume.getName());
            data.put("resume", r);
        } else {
            data.put("resume", null);
        }
        return data;
    }
}
