package com.aijobhunter.agent;

import com.aijobhunter.agent.model.GreetResult;
import com.aijobhunter.agent.model.JdAnalysisResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 打招呼 Agent */
@Slf4j
@Service
public class GreetAgent extends BaseAgent {

    @Override
    protected AgentType getAgentType() { return AgentType.GREET; }

    @Override
    protected String getDefaultSystemPrompt() {
        return "你是一个求职助手，擅长写打招呼话术。生成简洁、友好、有吸引力的内容，控制在 100-200 字。";
    }

    @Override
    protected String getDefaultUserPromptTemplate() {
        return """
            请为以下场景生成打招呼话术：
            ## 求职者信息
            姓名：{{candidate_name}}
            核心技能：{{top_skills}}
            工作经历亮点：{{experience_highlights}}
            工作年限：{{total_years}} 年

            ## 岗位信息
            岗位名称：{{job_title}}
            公司名称：{{company_name}}
            核心要求：{{must_have_skills}}
            岗位亮点：{{job_highlights}}

            ## 用户偏好
            打招呼风格：{{greeting_style}}

            输出格式（JSON）：
            {"greetingText": "话术", "length": 长度, "style": "风格", "highlightedPoints": ["亮点"]}
            """;
    }

    public AgentResult<GreetResult> generate(String candidateName, List<String> topSkills, String experienceHighlights,
            Integer totalYears, String jobTitle, String companyName, JdAnalysisResult jdAnalysis, String greetingStyle) {
        log.info("Generating greeting for {} @ {}", candidateName, companyName);
        Map<String, Object> vars = new HashMap<>();
        vars.put("candidate_name", candidateName);
        vars.put("top_skills", topSkills != null ? String.join(", ", topSkills) : "");
        vars.put("experience_highlights", experienceHighlights);
        vars.put("total_years", totalYears);
        vars.put("job_title", jobTitle);
        vars.put("company_name", companyName);
        vars.put("must_have_skills", jdAnalysis.getMustHaveSkills() != null ? String.join(", ", jdAnalysis.getMustHaveSkills()) : "");
        vars.put("job_highlights", jdAnalysis.getHighlights() != null ? String.join(", ", jdAnalysis.getHighlights()) : "");
        vars.put("greeting_style", greetingStyle != null ? greetingStyle : "friendly");
        return execute(vars, GreetResult.class);
    }

    public GreetResult degradeGenerate(String candidateName, String jobTitle, String companyName) {
        log.warn("Using degraded greeting");
        String text = String.format("您好！看到贵司在招聘 %s，很感兴趣，希望能聊聊～", jobTitle);
        return GreetResult.builder().greetingText(text).length(text.length()).style("friendly")
                .highlightedPoints(List.of("（降级模式）")).build();
    }
}
