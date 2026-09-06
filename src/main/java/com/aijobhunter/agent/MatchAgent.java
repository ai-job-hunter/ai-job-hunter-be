package com.aijobhunter.agent;

import com.aijobhunter.agent.model.JdAnalysisResult;
import com.aijobhunter.agent.model.MatchResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 匹配 Agent */
@Slf4j
@Service
public class MatchAgent extends BaseAgent {

    @Override
    protected AgentType getAgentType() { return AgentType.MATCH; }

    @Override
    protected String getDefaultSystemPrompt() {
        return "你是一个专业的简历匹配顾问，擅长评估简历与岗位的匹配度。请始终以 JSON 格式输出。";
    }

    @Override
    protected String getDefaultUserPromptTemplate() {
        return """
            请分析简历与岗位的匹配度：
            ## 简历信息
            姓名：{{candidate_name}}
            技能：{{skills}}
            工作经验：{{experience_summary}}
            工作年限：{{total_years}} 年

            ## 岗位要求
            岗位名称：{{job_title}}
            必须技能：{{must_have_skills}}
            加分技能：{{nice_to_have_skills}}
            最低经验：{{min_experience_years}} 年

            请输出匹配分析（JSON 格式）：
            {
              "overallScore": 总分（0-100）,
              "skillMatch": {"score": 分数, "matched": [], "missing": [], "partialMatch": []},
              "experienceMatch": {"score": 分数, "yearsMatch": true, "levelMatch": true, "assessment": ""},
              "educationMatch": {"score": 分数, "assessment": ""},
              "recommendation": "APPLY | SKIP | REVIEW",
              "reason": "建议原因",
              "tips": ["建议"]
            }
            """;
    }

    public AgentResult<MatchResult> analyze(String candidateName, List<String> skills, String experienceSummary,
            Integer totalYears, String jobTitle, JdAnalysisResult jdAnalysis) {
        log.info("Analyzing match for {} @ {}", candidateName, jobTitle);
        Map<String, Object> vars = new HashMap<>();
        vars.put("candidate_name", candidateName);
        vars.put("skills", String.join(", ", skills));
        vars.put("experience_summary", experienceSummary);
        vars.put("total_years", totalYears);
        vars.put("job_title", jobTitle);
        vars.put("must_have_skills", jdAnalysis.getMustHaveSkills() != null ? String.join(", ", jdAnalysis.getMustHaveSkills()) : "");
        vars.put("nice_to_have_skills", jdAnalysis.getNiceToHaveSkills() != null ? String.join(", ", jdAnalysis.getNiceToHaveSkills()) : "");
        vars.put("min_experience_years", jdAnalysis.getMinExperienceYears());
        return execute(vars, MatchResult.class);
    }

    public MatchResult degradeAnalyze(List<String> resumeSkills, JdAnalysisResult jdAnalysis) {
        log.warn("Using degraded match analysis");
        return MatchResult.builder().overallScore(50).recommendation("REVIEW").reason("降级模式").build();
    }
}
