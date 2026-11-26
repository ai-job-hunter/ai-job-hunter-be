package com.aijobhunter.service;

import com.aijobhunter.domain.platform.PlatformConnection;
import com.aijobhunter.domain.platform.PlatformConnectionRepository;
import com.aijobhunter.domain.platform.PlatformTask;
import com.aijobhunter.domain.platform.PlatformTaskRepository;
import com.aijobhunter.dto.CreateJobRequest;
import com.aijobhunter.dto.JobDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * V2 平台连接与任务：MOCK + 本机导入 JD（不建独立 agent 仓、不爬站）。
 */
@Service
@RequiredArgsConstructor
public class PlatformService {

    private static final Set<String> ALLOWED_PLATFORMS = Set.of("MOCK", "LIEPIN", "BOSS");
    private static final Set<String> ALLOWED_ACTIONS = Set.of(
            "SYNC_JOBS", "DRAFT_GREET", "FILL_REPLY", "MARK_APPLIED");

    private final PlatformConnectionRepository connectionRepository;
    private final PlatformTaskRepository taskRepository;
    private final JobService jobService;

    @Transactional
    public Map<String, Object> createConnection(String userId, String platform) {
        String p = normalizePlatform(platform);
        if ("BOSS".equals(p) || "LIEPIN".equals(p)) {
            throw new IllegalArgumentException(
                    "平台「" + p + "」尚未开放接入；请先用 MOCK 走通半自动流程");
        }
        PlatformConnection existing = connectionRepository.findByUserIdAndPlatform(userId, p).orElse(null);
        if (existing != null) {
            existing.setStatus(PlatformConnection.ConnectionStatus.CONNECTED);
            Map<String, Object> meta = existing.getMeta() == null ? new HashMap<>() : new HashMap<>(existing.getMeta());
            meta.put("note", "mock 连接（无真实账号；可导入 JD）");
            existing.setMeta(meta);
            existing = connectionRepository.save(existing);
            return connectionDto(existing);
        }
        PlatformConnection conn = PlatformConnection.builder()
                .userId(userId)
                .platform(p)
                .status(PlatformConnection.ConnectionStatus.CONNECTED)
                .meta(Map.of("note", "mock 连接（无真实账号；可导入 JD）"))
                .build();
        return connectionDto(connectionRepository.save(conn));
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listConnections(String userId) {
        return connectionRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::connectionDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> createTask(
            String userId,
            String connectionId,
            String action,
            String mode,
            String rawDescription,
            String title,
            String companyName) {
        if (connectionId == null || connectionId.isBlank()) {
            throw new IllegalArgumentException("请选择平台连接");
        }
        PlatformConnection conn = connectionRepository.findByIdAndUserId(connectionId, userId)
                .orElseThrow(() -> new IllegalArgumentException("平台连接不存在"));
        if (conn.getStatus() != PlatformConnection.ConnectionStatus.CONNECTED) {
            throw new IllegalArgumentException("平台未连接");
        }
        String act = normalizeAction(action);
        PlatformTask.TaskMode taskMode = parseMode(mode);

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("platform", conn.getPlatform());
        if ("SYNC_JOBS".equals(act)) {
            if (rawDescription == null || rawDescription.isBlank()) {
                throw new IllegalArgumentException("导入岗位请粘贴 JD 原文");
            }
            request.put("hint", "确认后将 JD 写入岗位列表（source=IMPORT），不访问招聘站");
            request.put("rawDescription", rawDescription.trim());
            if (title != null && !title.isBlank()) {
                request.put("title", title.trim());
            }
            if (companyName != null && !companyName.isBlank()) {
                request.put("companyName", companyName.trim());
            }
        } else {
            request.put("hint", "mock：确认后仅标记完成，不会访问真实招聘站");
        }

        PlatformTask task = PlatformTask.builder()
                .userId(userId)
                .connectionId(conn.getId())
                .action(act)
                .mode(taskMode)
                .status(PlatformTask.TaskStatus.NEED_CONFIRM)
                .request(request)
                .build();
        return taskDto(taskRepository.save(task));
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listTasks(String userId) {
        return taskRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::taskDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> confirmTask(String userId, String taskId) {
        PlatformTask task = taskRepository.findByIdAndUserId(taskId, userId)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在"));
        if (task.getStatus() != PlatformTask.TaskStatus.NEED_CONFIRM
                && task.getStatus() != PlatformTask.TaskStatus.PENDING) {
            throw new IllegalStateException("当前状态不可确认: " + task.getStatus());
        }
        task.setStatus(PlatformTask.TaskStatus.RUNNING);
        taskRepository.save(task);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("mock", true);
        result.put("action", task.getAction());

        try {
            if ("SYNC_JOBS".equals(task.getAction())) {
                Map<String, Object> req = task.getRequest() == null ? Map.of() : task.getRequest();
                Object raw = req.get("rawDescription");
                if (raw == null || raw.toString().isBlank()) {
                    throw new IllegalArgumentException("任务缺少 JD 原文，无法导入");
                }
                CreateJobRequest jobReq = new CreateJobRequest();
                jobReq.setRawDescription(raw.toString());
                jobReq.setAnalyze(Boolean.FALSE);
                if (req.get("title") != null) {
                    jobReq.setTitle(req.get("title").toString());
                }
                if (req.get("companyName") != null) {
                    jobReq.setCompanyName(req.get("companyName").toString());
                }
                JobDTO job = jobService.createImported(userId, jobReq);
                result.put("message", "已导入岗位：" + job.getTitle());
                result.put("syncedJobs", 1);
                result.put("jobId", job.getId());
                result.put("jobTitle", job.getTitle());
            } else {
                result.put("message", "mock 已完成：" + task.getAction());
                result.put("syncedJobs", 0);
            }
            task.setResult(result);
            task.setError(null);
            task.setStatus(PlatformTask.TaskStatus.DONE);
        } catch (RuntimeException e) {
            task.setError(e.getMessage());
            task.setStatus(PlatformTask.TaskStatus.FAILED);
            result.put("message", e.getMessage());
            task.setResult(result);
            task = taskRepository.save(task);
            if (e instanceof IllegalArgumentException || e instanceof IllegalStateException) {
                throw e;
            }
            throw new IllegalStateException(e.getMessage() == null ? "任务执行失败" : e.getMessage(), e);
        }

        task = taskRepository.save(task);

        if (task.getConnectionId() != null) {
            connectionRepository.findByIdAndUserId(task.getConnectionId(), userId).ifPresent(c -> {
                c.setLastSyncAt(LocalDateTime.now());
                connectionRepository.save(c);
            });
        }
        return taskDto(task);
    }

    private static String normalizePlatform(String platform) {
        if (platform == null || platform.isBlank()) {
            throw new IllegalArgumentException("请指定平台（如 MOCK）");
        }
        String p = platform.trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_PLATFORMS.contains(p)) {
            throw new IllegalArgumentException("不支持的平台: " + platform);
        }
        return p;
    }

    private static String normalizeAction(String action) {
        if (action == null || action.isBlank()) {
            throw new IllegalArgumentException("请指定任务动作");
        }
        String a = action.trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_ACTIONS.contains(a)) {
            throw new IllegalArgumentException("不支持的动作: " + action);
        }
        return a;
    }

    private static PlatformTask.TaskMode parseMode(String mode) {
        if (mode == null || mode.isBlank()) {
            return PlatformTask.TaskMode.SEMI_AUTO;
        }
        try {
            PlatformTask.TaskMode m = PlatformTask.TaskMode.valueOf(mode.trim().toUpperCase(Locale.ROOT));
            if (m == PlatformTask.TaskMode.FULL_AUTO) {
                throw new IllegalArgumentException("当前仅支持 SEMI_AUTO");
            }
            return m;
        } catch (IllegalArgumentException e) {
            if (e.getMessage() != null && e.getMessage().contains("SEMI_AUTO")) {
                throw e;
            }
            throw new IllegalArgumentException("不支持的模式: " + mode);
        }
    }

    private Map<String, Object> connectionDto(PlatformConnection c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", c.getId());
        m.put("platform", c.getPlatform());
        m.put("status", c.getStatus() == null ? null : c.getStatus().name());
        m.put("meta", c.getMeta());
        m.put("lastSyncAt", c.getLastSyncAt());
        m.put("createdAt", c.getCreatedAt());
        return m;
    }

    private Map<String, Object> taskDto(PlatformTask t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", t.getId());
        m.put("connectionId", t.getConnectionId());
        m.put("action", t.getAction());
        m.put("mode", t.getMode() == null ? null : t.getMode().name());
        m.put("status", t.getStatus() == null ? null : t.getStatus().name());
        m.put("request", t.getRequest());
        m.put("result", t.getResult());
        m.put("error", t.getError());
        m.put("createdAt", t.getCreatedAt());
        m.put("updatedAt", t.getUpdatedAt());
        return m;
    }
}
