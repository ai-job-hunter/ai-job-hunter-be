package com.aijobhunter.service;

import com.aijobhunter.agent.GreetAgent;
import com.aijobhunter.agent.JdAnalyzerAgent;
import com.aijobhunter.agent.MatchAgent;
import com.aijobhunter.agent.model.GreetResult;
import com.aijobhunter.agent.model.JdAnalysisResult;
import com.aijobhunter.agent.model.MatchResult;
import com.aijobhunter.domain.job.Job;
import com.aijobhunter.domain.job.JobRepository;
import com.aijobhunter.domain.resume.Resume;
import com.aijobhunter.domain.resume.ResumeRepository;
import com.aijobhunter.domain.user.User;
import com.aijobhunter.dto.CreateJobRequest;
import com.aijobhunter.dto.JobDTO;
import com.aijobhunter.exception.AgentException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobService {

    private final JobRepository jobRepository;
    private final ResumeRepository resumeRepository;
    private final JdAnalyzerAgent jdAnalyzerAgent;
    private final MatchAgent matchAgent;
    private final GreetAgent greetAgent;
    private final ObjectMapper objectMapper;

    @Transactional
    public JobDTO create(String userId, CreateJobRequest request) {
        if (request.getRawDescription() == null || request.getRawDescription().isBlank()) {
            throw new IllegalArgumentException("请粘贴岗位 JD 原文");
        }
        String title = blankToNull(request.getTitle());
        if (title == null) {
            title = guessTitle(request.getRawDescription());
        }

        Job job = Job.builder()
                .userId(userId)
                .source("MANUAL")
                .sourceUrl(blankToNull(request.getSourceUrl()))
                .title(title)
                .companyName(blankToNull(request.getCompanyName()))
                .city(blankToNull(request.getCity()))
                .salaryText(blankToNull(request.getSalaryText()))
                .rawDescription(request.getRawDescription().trim())
                .status(Job.JobStatus.NEW)
                .analysis(Map.of())
                .matchScores(Map.of())
                .keywords(List.of())
                .build();
        job = jobRepository.save(job);

        boolean doAnalyze = request.getAnalyze() == null || Boolean.TRUE.equals(request.getAnalyze());
        if (doAnalyze) {
            job = runAnalyze(job);
        }
        return toDto(job, null);
    }

    @Transactional(readOnly = true)
    public List<JobDTO> list(String userId) {
        return jobRepository.findByUserIdOrderByDiscoveredAtDesc(userId).stream()
                .map(j -> toDto(j, pickLatestScore(j)))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public JobDTO get(String userId, String id) {
        Job job = requireJob(userId, id);
        return toDto(job, pickLatestScore(job));
    }

    @Transactional
    public void delete(String userId, String id) {
        Job job = requireJob(userId, id);
        jobRepository.delete(job);
    }

    @Transactional
    public JobDTO analyze(String userId, String id) {
        Job job = runAnalyze(requireJob(userId, id));
        return toDto(job, pickLatestScore(job));
    }

    @Transactional
    public Map<String, Object> match(String userId, String jobId, String resumeId) {
        Job job = requireJob(userId, jobId);
        Resume resume = resumeRepository.findByIdAndUserId(resumeId, userId)
                .orElseThrow(() -> new IllegalArgumentException("简历不存在"));

        JdAnalysisResult jd = analysisFromJob(job);
        if (jd.getMustHaveSkills() == null || jd.getMustHaveSkills().isEmpty()) {
            job = runAnalyze(job);
            jd = analysisFromJob(job);
        }

        String candidateName = userId;
        List<String> skills = extractSkills(resume);
        String experience = extractExperience(resume);
        Integer years = guessYears(resume);

        MatchResult result;
        try {
            result = matchAgent.analyze(candidateName, skills, experience, years, job.getTitle(), jd).getData();
        } catch (AgentException e) {
            log.warn("MatchAgent failed, degrade: {}", e.getMessage());
            result = matchAgent.degradeAnalyze(skills, jd);
        }

        Map<String, Object> scoreMap = job.getMatchScores() == null
                ? new HashMap<>()
                : new HashMap<>(job.getMatchScores());
        scoreMap.put(resumeId, objectMapper.convertValue(result, Map.class));
        job.setMatchScores(scoreMap);
        if (job.getStatus() == Job.JobStatus.NEW) {
            job.setStatus(Job.JobStatus.ANALYZED);
        }
        jobRepository.save(job);

        Map<String, Object> data = new HashMap<>();
        data.put("jobId", job.getId());
        data.put("resumeId", resumeId);
        data.put("overallScore", result.getOverallScore());
        data.put("skillMatch", result.getSkillMatch());
        data.put("experienceMatch", result.getExperienceMatch());
        data.put("educationMatch", result.getEducationMatch());
        data.put("recommendation", result.getRecommendation());
        data.put("reason", result.getReason());
        data.put("tips", result.getTips());
        return data;
    }

    @Transactional
    public Map<String, Object> greet(String userId, String jobId, String resumeId, String style, User user) {
        Job job = requireJob(userId, jobId);
        JdAnalysisResult jd = analysisFromJob(job);
        if (jd.getMustHaveSkills() == null || jd.getMustHaveSkills().isEmpty()) {
            job = runAnalyze(job);
            jd = analysisFromJob(job);
        }

        List<String> skills = List.of();
        String highlights = "";
        Integer years = 0;
        String candidateName = user.getUsername() != null ? user.getUsername() : "求职者";
        if (resumeId != null && !resumeId.isBlank()) {
            Resume resume = resumeRepository.findByIdAndUserId(resumeId, userId)
                    .orElseThrow(() -> new IllegalArgumentException("简历不存在"));
            skills = extractSkills(resume);
            highlights = extractExperience(resume);
            years = guessYears(resume);
        }

        GreetResult result;
        try {
            result = greetAgent.generate(
                    candidateName, skills, highlights, years,
                    job.getTitle(), job.getCompanyName() != null ? job.getCompanyName() : "",
                    jd, style).getData();
        } catch (AgentException e) {
            log.warn("GreetAgent failed, degrade: {}", e.getMessage());
            result = greetAgent.degradeGenerate(candidateName, job.getTitle(),
                    job.getCompanyName() != null ? job.getCompanyName() : "");
        }

        job.setGreetingText(result.getGreetingText());
        if (job.getStatus() == Job.JobStatus.NEW || job.getStatus() == Job.JobStatus.ANALYZED) {
            job.setStatus(Job.JobStatus.GREETED);
        }
        jobRepository.save(job);

        Map<String, Object> data = new HashMap<>();
        data.put("jobId", job.getId());
        data.put("greetingText", result.getGreetingText());
        data.put("length", result.getLength());
        data.put("style", result.getStyle());
        data.put("highlightedPoints", result.getHighlightedPoints());
        return data;
    }

    private Job runAnalyze(Job job) {
        JdAnalysisResult result;
        try {
            result = jdAnalyzerAgent.analyze(
                    job.getTitle(),
                    job.getCompanyName() != null ? job.getCompanyName() : "",
                    job.getRawDescription()).getData();
        } catch (AgentException e) {
            log.warn("JdAnalyzerAgent failed, degrade: {}", e.getMessage());
            result = jdAnalyzerAgent.degradeAnalyze(job.getRawDescription());
            // 简易关键词：从原文抽常见技术词
            result.setSkills(simpleKeywordExtract(job.getRawDescription()));
            result.setMustHaveSkills(result.getSkills());
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> analysisMap = objectMapper.convertValue(result, Map.class);
        job.setAnalysis(analysisMap);
        List<String> keywords = result.getSkills() != null ? result.getSkills() : List.of();
        if (keywords.isEmpty() && result.getMustHaveSkills() != null) {
            keywords = result.getMustHaveSkills();
        }
        job.setKeywords(keywords);
        if (job.getStatus() == Job.JobStatus.NEW) {
            job.setStatus(Job.JobStatus.ANALYZED);
        }
        if (result.getSalaryRange() != null && !result.getSalaryRange().isBlank()
                && (job.getSalaryText() == null || job.getSalaryText().isBlank())) {
            job.setSalaryText(result.getSalaryRange());
        }
        return jobRepository.save(job);
    }

    private Job requireJob(String userId, String id) {
        return jobRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("岗位不存在"));
    }

    private JdAnalysisResult analysisFromJob(Job job) {
        if (job.getAnalysis() == null || job.getAnalysis().isEmpty()) {
            return JdAnalysisResult.builder()
                    .skills(List.of())
                    .mustHaveSkills(List.of())
                    .niceToHaveSkills(List.of())
                    .highlights(List.of())
                    .build();
        }
        return objectMapper.convertValue(job.getAnalysis(), JdAnalysisResult.class);
    }

    private JobDTO toDto(Job job, Integer matchScore) {
        return JobDTO.builder()
                .id(job.getId())
                .source(job.getSource())
                .sourceUrl(job.getSourceUrl())
                .title(job.getTitle())
                .companyName(job.getCompanyName())
                .city(job.getCity())
                .salaryText(job.getSalaryText())
                .rawDescription(job.getRawDescription())
                .analysis(job.getAnalysis())
                .keywords(job.getKeywords())
                .matchScores(job.getMatchScores())
                .status(job.getStatus() != null ? job.getStatus().name() : null)
                .greetingText(job.getGreetingText())
                .matchScore(matchScore)
                .discoveredAt(job.getDiscoveredAt())
                .createdAt(job.getCreatedAt())
                .updatedAt(job.getUpdatedAt())
                .build();
    }

    @SuppressWarnings("unchecked")
    private Integer pickLatestScore(Job job) {
        if (job.getMatchScores() == null || job.getMatchScores().isEmpty()) {
            return null;
        }
        Integer best = null;
        for (Object v : job.getMatchScores().values()) {
            if (v instanceof Map<?, ?> m && m.get("overallScore") instanceof Number n) {
                int score = n.intValue();
                if (best == null || score > best) {
                    best = score;
                }
            }
        }
        return best;
    }

    private static String blankToNull(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        return s.trim();
    }

    private static String guessTitle(String raw) {
        String first = raw.lines().map(String::trim).filter(l -> !l.isEmpty()).findFirst().orElse("未命名岗位");
        return first.length() > 80 ? first.substring(0, 80) : first;
    }

    private static List<String> extractSkills(Resume resume) {
        Map<String, Object> parsed = resume.getParsedData();
        if (parsed == null) {
            return List.of();
        }
        Object skills = parsed.get("skills");
        if (skills instanceof List<?> list) {
            List<String> out = new ArrayList<>();
            for (Object o : list) {
                if (o != null) {
                    out.add(o.toString());
                }
            }
            return out;
        }
        Object raw = parsed.get("rawText");
        if (raw != null) {
            return simpleKeywordExtract(raw.toString());
        }
        Object summary = parsed.get("summary");
        if (summary != null) {
            return simpleKeywordExtract(summary.toString());
        }
        return List.of();
    }

    private static String extractExperience(Resume resume) {
        Map<String, Object> parsed = resume.getParsedData();
        if (parsed == null) {
            return "";
        }
        Object summary = parsed.get("summary");
        if (summary != null) {
            return truncate(summary.toString(), 800);
        }
        Object raw = parsed.get("rawText");
        return raw != null ? truncate(raw.toString(), 800) : "";
    }

    private static Integer guessYears(Resume resume) {
        String text = extractExperience(resume).toLowerCase(Locale.ROOT);
        // 粗略：找「N年」
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+)\\s*年").matcher(text);
        if (m.find()) {
            try {
                return Integer.parseInt(m.group(1));
            } catch (NumberFormatException ignored) {
                // fall through
            }
        }
        return 0;
    }

    private static List<String> simpleKeywordExtract(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        String lower = text.toLowerCase(Locale.ROOT);
        String[] dict = {
                "java", "spring", "spring boot", "spring cloud", "mysql", "redis", "kafka",
                "kubernetes", "k8s", "docker", "python", "go", "golang", "react", "vue",
                "typescript", "nodejs", "postgresql", "elasticsearch", "rabbitmq", "微服务"
        };
        List<String> found = new ArrayList<>();
        for (String word : dict) {
            if (lower.contains(word)) {
                found.add(word.contains(" ") ? capitalizeWords(word) : (
                        word.equals("java") ? "Java"
                                : word.equals("mysql") ? "MySQL"
                                : word.equals("redis") ? "Redis"
                                : word.equals("kafka") ? "Kafka"
                                : word.equals("docker") ? "Docker"
                                : word.equals("python") ? "Python"
                                : word.equals("react") ? "React"
                                : word.equals("vue") ? "Vue"
                                : word.equals("typescript") ? "TypeScript"
                                : word.equals("postgresql") ? "PostgreSQL"
                                : word.equals("elasticsearch") ? "Elasticsearch"
                                : word.equals("rabbitmq") ? "RabbitMQ"
                                : word.equals("微服务") ? "微服务"
                                : word.equals("k8s") || word.equals("kubernetes") ? "Kubernetes"
                                : word.equals("go") || word.equals("golang") ? "Go"
                                : word.equals("nodejs") ? "Node.js"
                                : word.startsWith("spring") ? capitalizeWords(word)
                                : word));
            }
        }
        return found.stream().distinct().limit(20).collect(Collectors.toList());
    }

    private static String capitalizeWords(String s) {
        String[] parts = s.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.isEmpty()) continue;
            if (!sb.isEmpty()) sb.append(' ');
            sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));
        }
        return sb.toString();
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max);
    }
}
