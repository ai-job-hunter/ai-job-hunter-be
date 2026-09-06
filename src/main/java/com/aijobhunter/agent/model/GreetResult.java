package com.aijobhunter.agent.model;

import lombok.*;
import java.util.List;

/** 打招呼话术结果 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GreetResult {
    private String greetingText;
    private Integer length;
    private String style;
    private List<String> highlightedPoints;
}
