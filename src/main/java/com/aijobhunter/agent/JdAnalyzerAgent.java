package com.aijobhunter.agent;

import com.aijobhunter.agent.model.JdAnalysisResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/** JD 分析 Agent */
@Slf4j
@Service
public class JdAnalyzerAgent extends BaseAgent {

    @Override
    protected AgentType getAgentType() { return AgentType.JD_ANALYZER; }

    @Override
    protected String getDefaultSystemPrompt() {
        return "你是一个专业的 HR 助手，擅长分析招聘 JD。请提取关键信息，始终以 JSON 格式输出。";
    }

    @Override
    protected String getDefaultUserPromptTemplate() {
        return """
            请分析以下岗位 JD：
            岗位名称：{{job_title}}
            公司名称：{{company_name}}
            JD 内容：
            {{raw_description}}

            请提取以下信息（JSON 格式）：
            {
              "skills": ["核心技能列表"],
              "responsibilities": ["主要职责"],
              "requirements": ["任职要求"],
              "highlights": ["岗位亮点"],
              "minExperienceYears": 最低年限（数字）,
              "education": 学历要求,
              "mustHaveSkills": ["硬性技能"],
              "niceToHaveSkills": ["加分技能"],
              "salaryRange": "薪资范围",
              "redFlags": ["需要注意的问题"]
            }
            """;
    }

    public AgentResult<JdAnalysisResult> analyze(String jobTitle, String companyName, String rawDescription) {
        log.info("Analyzing JD: {} @ {}", jobTitle, companyName);
        Map<String, Object> vars = new HashMap<>();
        vars.put("job_title", jobTitle);
        vars.put("company_name", companyName);
        vars.put("raw_description", rawDescription);
        return execute(vars, JdAnalysisResult.class);
    }

    public JdAnalysisResult degradeAnalyze(String rawDescription) {
        log.warn("Using degraded JD analysis");
        String lower = rawDescription.toLowerCase();
        return JdAnalysisResult.builder()
                .skills(java.util.List.of())
                .redFlags(java.util.List.of("（降级模式）"))
                .build();
    }
}
