package com.aijobhunter.service;

import com.aijobhunter.agent.ResumeAgent;
import com.aijobhunter.agent.ResumeRewriteAgent;
import com.aijobhunter.agent.model.ResumeAnalysisResult;
import com.aijobhunter.agent.model.ResumeRewriteResult;
import com.aijobhunter.domain.job.Job;
import com.aijobhunter.domain.job.JobRepository;
import com.aijobhunter.domain.resume.Resume;
import com.aijobhunter.domain.resume.ResumeRepository;
import com.aijobhunter.dto.AnalyzeResumeRequest;
import com.aijobhunter.dto.ResumeDTO;
import com.aijobhunter.dto.RewriteResumeRequest;
import com.aijobhunter.dto.UpdateResumeRequest;
import com.aijobhunter.exception.AgentException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeService {

    private static final Set<String> ALLOWED_EXT = Set.of("pdf", "txt", "doc", "docx");
    private static final long MAX_BYTES = 10L * 1024 * 1024;

    private final ResumeRepository resumeRepository;
    private final JobRepository jobRepository;
    private final ResumeAgent resumeAgent;
    private final ResumeRewriteAgent resumeRewriteAgent;
    private final ObjectMapper objectMapper;

    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    @Transactional
    public ResumeDTO upload(String userId, MultipartFile file, String name, String type, Boolean isDefault)
            throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请选择简历文件");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("简历名称不能为空");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new IllegalArgumentException("文件不能超过 10MB");
        }

        String original = file.getOriginalFilename() == null ? "resume.bin" : file.getOriginalFilename();
        String ext = extensionOf(original);
        if (!ALLOWED_EXT.contains(ext)) {
            throw new IllegalArgumentException("仅支持 pdf / txt / doc / docx");
        }

        Resume.ResumeType resumeType = parseType(type);
        boolean makeDefault = Boolean.TRUE.equals(isDefault) || resumeRepository.countByUserId(userId) == 0;
        if (makeDefault) {
            resumeRepository.clearDefaultForUser(userId);
        }

        Path userDir = Path.of(uploadDir, userId).toAbsolutePath().normalize();
        Files.createDirectories(userDir);
        String storedName = UUID.randomUUID() + "_" + sanitizeFilename(original);
        Path target = userDir.resolve(storedName);
        Files.copy(file.getInputStream(), target);

        Map<String, Object> fileInfo = new HashMap<>();
        fileInfo.put("originalName", original);
        fileInfo.put("storagePath", target.toString());
        fileInfo.put("mimeType", file.getContentType() == null ? "application/octet-stream" : file.getContentType());
        fileInfo.put("size", file.getSize());

        Map<String, Object> parsed = new HashMap<>();
        Resume.ResumeStatus status = Resume.ResumeStatus.UPLOADED;
        if ("txt".equals(ext)) {
            String text = Files.readString(target, StandardCharsets.UTF_8);
            parsed.put("rawText", truncate(text, 50_000));
            parsed.put("summary", truncate(text, 500));
            status = Resume.ResumeStatus.PARSED;
        } else if ("pdf".equals(ext)) {
            String text = extractPdfText(target);
            if (text != null && !text.isBlank()) {
                parsed.put("rawText", truncate(text, 50_000));
                parsed.put("summary", truncate(text, 500));
                parsed.put("parser", "pdfbox-3.0.8");
                status = Resume.ResumeStatus.PARSED;
            } else {
                parsed.put("note", "PDF 已保存，但未能抽出有效文本（可能是扫描件）");
            }
        } else {
            parsed.put("note", "doc/docx 已保存，结构化解析后续接入；建议先用 txt 或 PDF");
        }

        Resume resume = Resume.builder()
                .userId(userId)
                .name(name.trim())
                .type(resumeType)
                .isDefault(makeDefault)
                .fileInfo(fileInfo)
                .parsedData(parsed)
                .analysis(Map.of())
                .status(status)
                .build();

        return toDto(resumeRepository.save(resume));
    }

    @Transactional(readOnly = true)
    public List<ResumeDTO> list(String userId, String type) {
        List<Resume> list;
        if (type != null && !type.isBlank()) {
            list = resumeRepository.findByUserIdAndTypeOrderByCreatedAtDesc(userId, parseType(type));
        } else {
            list = resumeRepository.findByUserIdOrderByCreatedAtDesc(userId);
        }
        return list.stream().map(ResumeService::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ResumeDTO get(String userId, String id) {
        Resume resume = resumeRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("简历不存在"));
        return toDto(resume);
    }

    @Transactional
    public ResumeDTO update(String userId, String id, UpdateResumeRequest request) {
        Resume resume = resumeRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("简历不存在"));

        if (request.getName() != null && !request.getName().isBlank()) {
            resume.setName(request.getName().trim());
        }
        if (request.getType() != null && !request.getType().isBlank()) {
            resume.setType(parseType(request.getType()));
        }
        if (Boolean.TRUE.equals(request.getIsDefault())) {
            resumeRepository.clearDefaultForUser(userId);
            resume.setIsDefault(true);
        } else if (Boolean.FALSE.equals(request.getIsDefault())) {
            resume.setIsDefault(false);
        }

        return toDto(resumeRepository.save(resume));
    }

    @Transactional
    public void delete(String userId, String id) {
        Resume resume = resumeRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("简历不存在"));
        Object path = resume.getFileInfo() == null ? null : resume.getFileInfo().get("storagePath");
        resumeRepository.delete(resume);
        if (path instanceof String p) {
            try {
                Files.deleteIfExists(Path.of(p));
            } catch (IOException ignored) {
                // 文件清理失败不影响主流程
            }
        }
    }

    /**
     * 同步 AI 分析简历；LLM 失败时降级写入基础评估。
     */
    @Transactional
    public ResumeDTO analyze(String userId, String id, AnalyzeResumeRequest request) {
        Resume resume = requireResume(userId, id);
        String content = extractText(resume);
        String targetJob = resolveTargetJob(userId, request == null ? null : request.getTargetJobId(),
                request == null ? null : request.getTargetJob());

        ResumeAnalysisResult result;
        try {
            result = resumeAgent.analyze(content, targetJob).getData();
        } catch (AgentException e) {
            log.warn("ResumeAgent failed, degrade: {}", e.getMessage());
            result = resumeAgent.degradeAnalyze();
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> analysisMap = objectMapper.convertValue(result, Map.class);
        resume.setAnalysis(analysisMap);
        resume.setStatus(Resume.ResumeStatus.ANALYZED);

        // 把技能写回 parsedData，供岗位匹配复用
        if (result.getSkillAnalysis() != null && result.getSkillAnalysis().getTechnicalSkills() != null) {
            Map<String, Object> parsed = resume.getParsedData() == null
                    ? new HashMap<>()
                    : new HashMap<>(resume.getParsedData());
            parsed.put("skills", result.getSkillAnalysis().getTechnicalSkills());
            resume.setParsedData(parsed);
        }

        return toDto(resumeRepository.save(resume));
    }

    /**
     * 同步 AI 重写建议；结果写入 analysis.rewrite，不覆盖原文。
     */
    @Transactional
    public ResumeDTO rewrite(String userId, String id, RewriteResumeRequest request) {
        Resume resume = requireResume(userId, id);
        String content = extractText(resume);
        String jobDesc = "";
        if (request != null && request.getTargetJobId() != null && !request.getTargetJobId().isBlank()) {
            Job job = jobRepository.findByIdAndUserId(request.getTargetJobId(), userId)
                    .orElseThrow(() -> new IllegalArgumentException("目标岗位不存在"));
            jobDesc = (job.getTitle() == null ? "" : job.getTitle() + "\n")
                    + (job.getRawDescription() == null ? "" : job.getRawDescription());
        }
        String special = request == null ? null : request.getSpecialRequirements();
        if (request != null && request.getTone() != null && !request.getTone().isBlank()) {
            String toneNote = "语气风格：" + request.getTone();
            special = special == null || special.isBlank() ? toneNote : special + "；" + toneNote;
        }

        ResumeRewriteResult result;
        try {
            result = resumeRewriteAgent.rewrite(content, jobDesc.isBlank() ? "（未指定岗位）" : jobDesc, special)
                    .getData();
        } catch (AgentException e) {
            log.warn("ResumeRewriteAgent failed, degrade: {}", e.getMessage());
            result = resumeRewriteAgent.degradeRewrite();
        }

        Map<String, Object> analysis = resume.getAnalysis() == null
                ? new HashMap<>()
                : new HashMap<>(resume.getAnalysis());
        @SuppressWarnings("unchecked")
        Map<String, Object> rewriteMap = objectMapper.convertValue(result, Map.class);
        analysis.put("rewrite", rewriteMap);
        resume.setAnalysis(analysis);

        return toDto(resumeRepository.save(resume));
    }

    private Resume requireResume(String userId, String id) {
        return resumeRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("简历不存在"));
    }

    private String resolveTargetJob(String userId, String jobId, String targetJobText) {
        if (jobId != null && !jobId.isBlank()) {
            Job job = jobRepository.findByIdAndUserId(jobId, userId)
                    .orElseThrow(() -> new IllegalArgumentException("目标岗位不存在"));
            return (job.getTitle() == null ? "" : job.getTitle() + "\n")
                    + (job.getRawDescription() == null ? "" : job.getRawDescription());
        }
        if (targetJobText != null && !targetJobText.isBlank()) {
            return targetJobText.trim();
        }
        return "（未指定）";
    }

    private static String extractText(Resume resume) {
        Map<String, Object> parsed = resume.getParsedData();
        if (parsed != null) {
            Object raw = parsed.get("rawText");
            if (raw instanceof String s && !s.isBlank()) {
                return truncate(s, 40_000);
            }
            Object summary = parsed.get("summary");
            if (summary instanceof String s && !s.isBlank()) {
                return truncate(s, 40_000);
            }
        }
        throw new IllegalArgumentException("当前简历无可分析文本，请上传 txt/PDF，或确认 PDF 非扫描件");
    }

    /** PDFBox 3.x：Loader.loadPDF；失败返回 null，不阻断上传。 */
    private static String extractPdfText(Path pdfPath) {
        try (var doc = org.apache.pdfbox.Loader.loadPDF(pdfPath.toFile())) {
            var stripper = new org.apache.pdfbox.text.PDFTextStripper();
            String text = stripper.getText(doc);
            if (text == null) {
                return null;
            }
            return text.replace('\u0000', ' ').trim();
        } catch (Exception e) {
            log.warn("PDF text extract failed: {}", e.getMessage());
            return null;
        }
    }

    private static Resume.ResumeType parseType(String type) {
        if (type == null || type.isBlank()) {
            return Resume.ResumeType.GENERAL;
        }
        try {
            return Resume.ResumeType.valueOf(type.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("不支持的简历类型: " + type);
        }
    }

    private static String extensionOf(String filename) {
        int i = filename.lastIndexOf('.');
        if (i < 0 || i == filename.length() - 1) {
            return "";
        }
        return filename.substring(i + 1).toLowerCase(Locale.ROOT);
    }

    private static String sanitizeFilename(String name) {
        return name.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max);
    }

    static ResumeDTO toDto(Resume r) {
        return ResumeDTO.builder()
                .id(r.getId())
                .name(r.getName())
                .type(r.getType() == null ? null : r.getType().name())
                .isDefault(r.getIsDefault())
                .status(r.getStatus() == null ? null : r.getStatus().name())
                .fileInfo(r.getFileInfo())
                .parsedData(r.getParsedData())
                .analysis(r.getAnalysis())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }
}
