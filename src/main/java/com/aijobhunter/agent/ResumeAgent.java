package com.aijobhunter.agent;

import com.aijobhunter.agent.model.ResumeAnalysisResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/** 简历分析 Agent */
@Slf4j
@Service
public class ResumeAgent extends BaseAgent {

    @Override
    protected AgentType getAgentType() { return AgentType.RESUME_ANALYSIS; }

    @Override
    protected String getDefaultSystemPrompt() {
        return "你是一个专业的简历顾问，擅长分析简历的优劣。识别优势亮点，指出问题不足，给出优化建议，始终以 JSON 格式输出。";
    }

    @Override
    protected String getDefaultUserPromptTemplate() {
        return """
            请分析以下简历：
            ## 简历内容
            {{resume_content}}
            ## 目标岗位
            {{target_job}}

            请从以下维度分析（JSON 格式）：
            {
              "overallAssessment": "整体评估",
              "strengths": [{"point": "", "evidence": "", "impact": ""}],
              "weaknesses": [{"point": "", "severity": "", "suggestion": ""}],
              "skillAnalysis": {"technicalSkills": [], "softSkills": [], "missingSkills": [], "irrelevantSkills": []},
              "suggestions": [{"priority": 1, "action": "", "reason": ""}],
              "score": {"structure": 80, "content": 80, "keywords": 80, "overall": 80}
            }
            """;
    }

    public AgentResult<ResumeAnalysisResult> analyze(String resumeContent, String targetJob) {
        log.info("Analyzing resume (target: {})", targetJob);
        Map<String, Object> vars = new HashMap<>();
        vars.put("resume_content", resumeContent);
        vars.put("target_job", targetJob != null ? targetJob : "（未指定）");
        return execute(vars, ResumeAnalysisResult.class);
    }

    public ResumeAnalysisResult degradeAnalyze() {
        log.warn("Using degraded resume analysis");
        return ResumeAnalysisResult.builder().overallAssessment("（降级模式）")
                .score(ResumeAnalysisResult.Score.builder().structure(70).content(70).keywords(70).overall(70).build())
                .build();
    }
}
