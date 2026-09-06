package com.aijobhunter.agent.model;

import lombok.*;
import java.util.List;

/** 简历重写结果 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeRewriteResult {
    private String summary;
    private List<String> skills;
    private List<RewrittenExperience> experience;
    private OptimizationNotes optimizationNotes;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class RewrittenExperience {
        private String company;
        private String position;
        private String duration;
        private List<String> highlights;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class OptimizationNotes {
        private List<String> added;
        private List<String> removed;
        private List<String> rewritten;
    }
}
