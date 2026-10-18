package com.aijobhunter.dto;

import lombok.Data;

@Data
public class CreateJobRequest {
    /** 岗位名称（可空，将从 JD 首行或默认「未命名岗位」） */
    private String title;
    private String companyName;
    private String city;
    private String salaryText;
    /** 可选：招聘页链接（仅存档，不爬取） */
    private String sourceUrl;
    /** JD 原文，必填 */
    private String rawDescription;
    /** 创建后是否立即调用 AI 分析，默认 true */
    private Boolean analyze;
}
