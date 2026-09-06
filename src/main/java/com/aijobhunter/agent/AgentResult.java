package com.aijobhunter.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Agent 执行结果基类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentResult<T> {

    private boolean success;
    private T data;
    private String error;
    private String model;
    private Integer totalTokens;
    private Long latencyMs;
    private boolean degraded;

    public static <T> AgentResult<T> success(T data) {
        return AgentResult.<T>builder().success(true).data(data).build();
    }

    public static <T> AgentResult<T> success(T data, String model, Integer tokens, Long latency) {
        return AgentResult.<T>builder()
                .success(true).data(data).model(model).totalTokens(tokens).latencyMs(latency).build();
    }

    public static <T> AgentResult<T> failure(String error) {
        return AgentResult.<T>builder().success(false).error(error).build();
    }

    public static <T> AgentResult<T> degraded(T data, String error) {
        return AgentResult.<T>builder().success(true).data(data).degraded(true).error(error).build();
    }
}
