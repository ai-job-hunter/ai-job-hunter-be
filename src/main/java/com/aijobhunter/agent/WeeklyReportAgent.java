package com.aijobhunter.agent;

import com.aijobhunter.agent.model.WeeklyReportResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 周报生成 Agent */
@Slf4j
@Service
public class WeeklyReportAgent extends BaseAgent {

    @Override
    protected AgentType getAgentType() { return AgentType.WEEKLY_REPORT; }

    @Override
    protected String getDefaultSystemPrompt() {
        return "你是一个求职复盘专家，擅长分析求职数据并给出建议。总结本周数据，分析回复率，给出下周建议，始终以 JSON 格式输出。";
    }

    @Override
    protected String getDefaultUserPromptTemplate() {
        return """
            请根据以下数据生成求职周报：
            ## 本周数据
            - 发现岗位：{{jobs_discovered}} 个
            - 打招呼数：{{greetings_sent}} 次
            - 回复数：{{greetings_replied}} 次
            - 投递数：{{applications_sent}} 次
            - 约面数：{{interviews_scheduled}} 次
            ## 与上周对比
            {{comparison_with_last_week}}
            ## 特别事件
            {{special_events}}

            请生成周报（JSON 格式）：
            {
              "title": "本周求职复盘",
              "content": "Markdown 格式周报内容",
              "overview": {"jobsDiscovered": 0, "greetingsSent": 0, "greetingsReplied": 0, "applicationsSent": 0, "interviewsScheduled": 0, "replyRate": 0.0, "interviewRate": 0.0},
              "nextWeekSuggestions": ["建议1", "建议2"]
            }
            """;
    }

    public AgentResult<WeeklyReportResult> generate(Integer jobsDiscovered, Integer greetingsSent,
            Integer greetingsReplied, Integer applicationsSent, Integer interviewsScheduled,
            String comparisonWithLastWeek, String specialEvents) {
        log.info("Generating weekly report");
        Map<String, Object> vars = new HashMap<>();
        vars.put("jobs_discovered", jobsDiscovered);
        vars.put("greetings_sent", greetingsSent);
        vars.put("greetings_replied", greetingsReplied);
        vars.put("applications_sent", applicationsSent);
        vars.put("interviews_scheduled", interviewsScheduled);
        vars.put("comparison_with_last_week", comparisonWithLastWeek != null ? comparisonWithLastWeek : "（首次）");
        vars.put("special_events", specialEvents != null ? specialEvents : "（无）");
        return execute(vars, WeeklyReportResult.class);
    }

    public WeeklyReportResult degradeGenerate(Integer greetingsSent, Integer greetingsReplied,
            Integer applicationsSent, Integer interviewsScheduled) {
        log.warn("Using degraded weekly report");
        double replyRate = greetingsSent > 0 ? greetingsReplied * 100.0 / greetingsSent : 0;
        String content = String.format("# 本周求职复盘\n\n回复率：%.1f%%\n\n（降级模式）", replyRate);
        return WeeklyReportResult.builder().title("本周求职复盘").content(content)
                .overview(WeeklyReportResult.WeeklyStats.builder()
                        .greetingsSent(greetingsSent).greetingsReplied(greetingsReplied)
                        .applicationsSent(applicationsSent).interviewsScheduled(interviewsScheduled)
                        .replyRate(replyRate).build())
                .nextWeekSuggestions(List.of("继续坚持每天打招呼", "关注 HR 回复")).build();
    }
}
