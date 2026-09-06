package com.aijobhunter.agent;

import com.aijobhunter.exception.AgentException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Agent 基类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public abstract class BaseAgent {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{(\\w+)}}");

    protected abstract AgentType getAgentType();
    protected abstract String getDefaultSystemPrompt();
    protected abstract String getDefaultUserPromptTemplate();

    public <T> AgentResult<T> execute(Map<String, Object> variables, Class<T> responseClass) {
        long startTime = System.currentTimeMillis();
        String model = "gpt-4o";

        try {
            String systemPrompt = getDefaultSystemPrompt();
            String userPrompt = getDefaultUserPromptTemplate();
            
            userPrompt = fillVariables(userPrompt, variables);
            log.debug("Executing {} with prompt length: {}", getAgentType(), userPrompt.length());

            String response = chatClient.prompt()
                    .system(systemPrompt)
                    .user(userPrompt)
                    .call()
                    .content();

            T result = parseResponse(response, responseClass);
            long latency = System.currentTimeMillis() - startTime;

            return AgentResult.<T>builder()
                    .success(true)
                    .data(result)
                    .model(model)
                    .latencyMs(latency)
                    .build();

        } catch (Exception e) {
            log.error("Agent {} execution failed: {}", getAgentType(), e.getMessage(), e);
            throw new AgentException(getAgentType().name(), e);
        }
    }

    protected String fillVariables(String template, Map<String, Object> variables) {
        if (template == null || variables == null) return template;
        String result = template;
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);
        while (matcher.find()) {
            String name = matcher.group(1);
            Object value = variables.get(name);
            result = result.replace("{{" + name + "}}", value != null ? value.toString() : "");
        }
        return result;
    }

    protected <T> T parseResponse(String response, Class<T> clazz) throws Exception {
        String json = extractJson(response);
        return objectMapper.readValue(json, clazz);
    }

    protected String extractJson(String response) {
        if (response.contains("```json")) {
            int start = response.indexOf("```json") + 7;
            int end = response.indexOf("```", start);
            if (end > start) return response.substring(start, end).trim();
        }
        if (response.contains("```")) {
            int start = response.indexOf("```") + 3;
            int end = response.indexOf("```", start);
            if (end > start) return response.substring(start, end).trim();
        }
        int start = response.indexOf('{');
        int end = response.lastIndexOf('}');
        if (start >= 0 && end > start) return response.substring(start, end + 1);
        start = response.indexOf('[');
        end = response.lastIndexOf(']');
        if (start >= 0 && end > start) return response.substring(start, end + 1);
        return response.trim();
    }
}
