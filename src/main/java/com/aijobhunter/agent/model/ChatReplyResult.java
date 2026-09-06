package com.aijobhunter.agent.model;

import lombok.*;
import java.util.List;

/** 聊天回复结果 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatReplyResult {
    private String replyText;
    private String intent;
    private Double confidence;
    private List<String> keyPoints;
    private List<AlternativeReply> alternativeReplies;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class AlternativeReply {
        private String content;
        private String tone;
    }
}
