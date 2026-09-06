package com.aijobhunter.agent.model;

import lombok.*;
import java.util.List;

/** 匹配分析结果 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchResult {
    private Integer overallScore;
    private SkillMatchDetail skillMatch;
    private ExperienceMatchDetail experienceMatch;
    private EducationMatchDetail educationMatch;
    private String recommendation;
    private String reason;
    private List<String> tips;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class SkillMatchDetail {
        private Integer score;
        private List<String> matched;
        private List<String> missing;
        private List<String> partialMatch;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ExperienceMatchDetail {
        private Integer score;
        private Boolean yearsMatch;
        private Boolean levelMatch;
        private String assessment;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class EducationMatchDetail {
        private Integer score;
        private String assessment;
    }
}
