package com.aijobhunter.agent;

import com.aijobhunter.agent.model.ChatReplyResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 聊天回复 Agent */
@Slf4j
@Service
public class ChatAgent extends BaseAgent {

    @Override
    protected AgentType getAgentType() { return AgentType.CHAT; }

    @Override
    protected String getDefaultSystemPrompt() {
        return "你是一个求职助手，擅长与 HR 沟通。分析意图，生成合适的回复话术，始终以 JSON 格式输出。";
    }

    @Override
    protected String getDefaultUserPromptTemplate() {
        return """
            请为以下 HR 消息生成回复：
            ## HR 消息
            "{{hr_message}}"
            ## 对话上下文
            {{conversation_history}}
            ## 求职者信息
            姓名：{{candidate_name}}
            简历亮点：{{resume_highlights}}
            期望薪资：{{expected_salary}}
            当前状态：{{current_status}}

            请生成回复话术（JSON 格式）：
            {"replyText": "回复", "intent": "意图", "confidence": 0.9, "keyPoints": [], "alternativeReplies": []}
            """;
    }

    public AgentResult<ChatReplyResult> generateReply(String hrMessage, String conversationHistory,
            String candidateName, String resumeHighlights, String jobPreference, String expectedSalary, String currentStatus) {
        log.info("Generating reply for HR message");
        Map<String, Object> vars = new HashMap<>();
        vars.put("hr_message", hrMessage);
        vars.put("conversation_history", conversationHistory != null ? conversationHistory : "（无）");
        vars.put("candidate_name", candidateName);
        vars.put("resume_highlights", resumeHighlights);
        vars.put("job_preference", jobPreference);
        vars.put("expected_salary", expectedSalary != null ? expectedSalary : "可谈");
        vars.put("current_status", currentStatus);
        return execute(vars, ChatReplyResult.class);
    }

    public ChatReplyResult degradeReply() {
        log.warn("Using degraded chat reply");
        return ChatReplyResult.builder().replyText("感谢您的回复！").intent("GENERAL").confidence(0.5)
                .keyPoints(List.of("（降级模式）")).build();
    }
}
