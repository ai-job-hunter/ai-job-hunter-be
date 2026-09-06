package com.aijobhunter.agent;

/**
 * Agent 类型枚举
 */
public enum AgentType {
    JD_ANALYZER("JD 分析 Agent", "分析招聘 JD，提取关键信息"),
    MATCH("匹配 Agent", "评估简历与岗位的匹配度"),
    GREET("打招呼 Agent", "生成个性化打招呼话术"),
    CHAT("聊天回复 Agent", "分析 HR 消息，生成回复建议"),
    RESUME_ANALYSIS("简历分析 Agent", "分析简历优势和不足"),
    RESUME_REWRITE("简历重写 Agent", "根据岗位优化简历"),
    WEEKLY_REPORT("周报生成 Agent", "生成求职周报和复盘建议");

    private final String displayName;
    private final String description;

    AgentType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
}
