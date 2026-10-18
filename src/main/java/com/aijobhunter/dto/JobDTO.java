package com.aijobhunter.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class JobDTO {
    private String id;
    private String source;
    private String sourceUrl;
    private String title;
    private String companyName;
    private String city;
    private String salaryText;
    private String rawDescription;
    private Map<String, Object> analysis;
    private List<String> keywords;
    private Map<String, Object> matchScores;
    private String status;
    private String greetingText;
    private Integer matchScore;
    private LocalDateTime discoveredAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
