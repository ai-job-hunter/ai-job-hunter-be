package com.aijobhunter.service;

import com.aijobhunter.domain.resume.Resume;
import com.aijobhunter.domain.resume.ResumeRepository;
import com.aijobhunter.dto.ResumeDTO;
import com.aijobhunter.dto.UpdateResumeRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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

@Service
@RequiredArgsConstructor
public class ResumeService {

    private static final Set<String> ALLOWED_EXT = Set.of("pdf", "txt", "doc", "docx");
    private static final long MAX_BYTES = 10L * 1024 * 1024;

    private final ResumeRepository resumeRepository;

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
        } else {
            parsed.put("note", "二进制简历已保存，结构化解析将在后续任务接入");
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
