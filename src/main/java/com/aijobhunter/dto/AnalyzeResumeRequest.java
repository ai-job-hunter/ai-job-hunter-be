package com.aijobhunter.dto;

import lombok.Data;

@Data
public class AnalyzeResumeRequest {
    /** 可选：对照某岗位做分析 */
    private String targetJobId;
    /** 可选：直接写目标岗位描述（无 jobId 时用） */
    private String targetJob;
}
