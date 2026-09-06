package com.aijobhunter.exception;

/**
 * Agent 执行异常
 */
public class AgentException extends RuntimeException {

    private final String agentType;
    private final String errorCode;

    public AgentException(String agentType, String message) {
        super(message);
        this.agentType = agentType;
        this.errorCode = "AGENT_ERROR";
    }

    public AgentException(String agentType, String message, Throwable cause) {
        super(message, cause);
        this.agentType = agentType;
        this.errorCode = "AGENT_ERROR";
    }

    public AgentException(String agentType, String errorCode, String message) {
        super(message);
        this.agentType = agentType;
        this.errorCode = errorCode;
    }

    public String getAgentType() { return agentType; }
    public String getErrorCode() { return errorCode; }
}
