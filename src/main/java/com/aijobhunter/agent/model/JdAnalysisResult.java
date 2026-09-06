package com.aijobhunter.agent.model;

import lombok.*;
import java.util.List;

/** JD 分析结果 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JdAnalysisResult {
    private List<String> skills;
    private List<String> responsibilities;
    private List<String> requirements;
    private List<String> highlights;
    private Integer minExperienceYears;
    private String education;
    private List<String> mustHaveSkills;
    private List<String> niceToHaveSkills;
    private String salaryRange;
    private List<String> redFlags;
}
