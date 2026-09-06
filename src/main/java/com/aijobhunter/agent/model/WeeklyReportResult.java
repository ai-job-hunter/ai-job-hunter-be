package com.aijobhunter.agent.model;

import lombok.*;
import java.util.List;

/** 周报生成结果 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeeklyReportResult {
    private String title;
    private String content;
    private WeeklyStats overview;
    private List<String> nextWeekSuggestions;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class WeeklyStats {
        private Integer jobsDiscovered;
        private Integer greetingsSent;
        private Integer greetingsReplied;
        private Integer applicationsSent;
        private Integer interviewsScheduled;
        private Double replyRate;
        private Double interviewRate;
    }
}
