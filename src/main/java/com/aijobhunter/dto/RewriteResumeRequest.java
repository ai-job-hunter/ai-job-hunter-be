package com.aijobhunter.dto;

import lombok.Data;

@Data
public class RewriteResumeRequest {
    /** 可选：针对某岗位 JD 优化 */
    private String targetJobId;
    /** 可选：额外优化要求 */
    private String specialRequirements;
    /** 可选：professional / casual / technical */
    private String tone;
}
