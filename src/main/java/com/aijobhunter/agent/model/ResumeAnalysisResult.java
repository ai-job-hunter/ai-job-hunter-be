package com.aijobhunter.agent.model;

import lombok.*;
import java.util.List;

/** 简历分析结果 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeAnalysisResult {
    private String overallAssessment;
    private List<Strength> strengths;
    private List<Weakness> weaknesses;
    private SkillAnalysis skillAnalysis;
    private List<Suggestion> suggestions;
    private Score score;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Strength {
        private String point;
        private String evidence;
        private String impact;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Weakness {
        private String point;
        private String severity;
        private String suggestion;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class SkillAnalysis {
        private List<String> technicalSkills;
        private List<String> softSkills;
        private List<String> missingSkills;
        private List<String> irrelevantSkills;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Suggestion {
        private Integer priority;
        private String action;
        private String reason;
    }

    @Data @Builder @NoArgsConstructor
    @AllArgsConstructor
    public static class Score {
        private Integer structure;
        private Integer content;
        private Integer keywords;
        private Integer overall;
    }
}
