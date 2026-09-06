package com.aijobhunter.agent;

import com.aijobhunter.agent.model.ResumeRewriteResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 简历重写 Agent */
@Slf4j
@Service
public class ResumeRewriteAgent extends BaseAgent {

    @Override
    protected AgentType getAgentType() { return AgentType.RESUME_REWRITE; }

    @Override
    protected String getDefaultSystemPrompt() {
        return "你是一个专业的简历优化专家，擅长根据岗位要求优化简历。突出相关经验，保持真实性，始终以 JSON 格式输出。";
    }

    @Override
    protected String getDefaultUserPromptTemplate() {
        return """
            请根据以下要求优化简历：
            ## 原简历
            {{resume_content}}
            ## 目标岗位 JD
            {{job_description}}
            ## 优化要求
            {{special_requirements}}

            请输出优化后的简历（JSON 格式）：
            {
              "summary": "自我介绍",
              "skills": ["技能列表"],
              "experience": [{"company": "", "position": "", "duration": "", "highlights": []}],
              "optimizationNotes": {"added": [], "removed": [], "rewritten": []}
            }
            """;
    }

    public AgentResult<ResumeRewriteResult> rewrite(String resumeContent, String jobDescription, String specialRequirements) {
        log.info("Rewriting resume");
        Map<String, Object> vars = new HashMap<>();
        vars.put("resume_content", resumeContent);
        vars.put("job_description", jobDescription);
        vars.put("special_requirements", specialRequirements != null ? specialRequirements : "无");
        return execute(vars, ResumeRewriteResult.class);
    }

    public ResumeRewriteResult degradeRewrite() {
        log.warn("Using degraded resume rewrite");
        return ResumeRewriteResult.builder().summary("（降级模式）")
                .optimizationNotes(ResumeRewriteResult.OptimizationNotes.builder()
                        .added(List.of()).removed(List.of()).rewritten(List.of("（降级）")).build())
                .build();
    }
}
