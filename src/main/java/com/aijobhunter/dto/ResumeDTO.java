package com.aijobhunter.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResumeDTO {
    private String id;
    private String name;
    private String type;
    private Boolean isDefault;
    private String status;
    private Map<String, Object> fileInfo;
    private Map<String, Object> parsedData;
    private Map<String, Object> analysis;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
