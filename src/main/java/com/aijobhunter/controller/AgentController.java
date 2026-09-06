package com.aijobhunter.controller;

import com.aijobhunter.agent.*;
import com.aijobhunter.agent.model.*;
import com.aijobhunter.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Agent API 控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/agent")
@RequiredArgsConstructor
public class AgentController {

    private final JdAnalyzerAgent jdAnalyzerAgent;
    private final MatchAgent matchAgent;
    private final GreetAgent greetAgent;
    private final ChatAgent chatAgent;
    private final ResumeAgent resumeAgent;
    private final ResumeRewriteAgent resumeRewriteAgent;
    private final WeeklyReportAgent weeklyReportAgent;

    @PostMapping("/jd/analyze")
    public ResponseEntity<ApiResponse<JdAnalysisResult>> analyzeJd(@RequestBody JdAnalyzeRequest request) {
        try {
            AgentResult<JdAnalysisResult> result = jdAnalyzerAgent.analyze(
                    request.getJobTitle(), request.getCompanyName(), request.getRawDescription());
            return ResponseEntity.ok(ApiResponse.success(result.getData()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/match")
    public ResponseEntity<ApiResponse<MatchResult>> analyzeMatch(@RequestBody MatchRequest request) {
        try {
            AgentResult<MatchResult> result = matchAgent.analyze(
                    request.getCandidateName(), request.getSkills(), request.getExperienceSummary(),
                    request.getTotalYears(), request.getJobTitle(), request.getJdAnalysis());
            return ResponseEntity.ok(ApiResponse.success(result.getData()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/greet")
    public ResponseEntity<ApiResponse<GreetResult>> generateGreeting(@RequestBody GreetRequest request) {
        try {
            AgentResult<GreetResult> result = greetAgent.generate(
                    request.getCandidateName(), request.getTopSkills(), request.getExperienceHighlights(),
                    request.getTotalYears(), request.getJobTitle(), request.getCompanyName(),
                    request.getJdAnalysis(), request.getGreetingStyle());
            return ResponseEntity.ok(ApiResponse.success(result.getData()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/chat/reply")
    public ResponseEntity<ApiResponse<ChatReplyResult>> generateReply(@RequestBody ChatReplyRequest request) {
        try {
            AgentResult<ChatReplyResult> result = chatAgent.generateReply(
                    request.getHrMessage(), request.getConversationHistory(),
                    request.getCandidateName(), request.getResumeHighlights(),
                    request.getJobPreference(), request.getExpectedSalary(), request.getCurrentStatus());
            return ResponseEntity.ok(ApiResponse.success(result.getData()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/resume/analyze")
    public ResponseEntity<ApiResponse<ResumeAnalysisResult>> analyzeResume(@RequestBody Map<String, String> request) {
        try {
            AgentResult<ResumeAnalysisResult> result = resumeAgent.analyze(
                    request.get("resumeContent"), request.get("targetJob"));
            return ResponseEntity.ok(ApiResponse.success(result.getData()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/resume/rewrite")
    public ResponseEntity<ApiResponse<ResumeRewriteResult>> rewriteResume(@RequestBody RewriteRequest request) {
        try {
            AgentResult<ResumeRewriteResult> result = resumeRewriteAgent.rewrite(
                    request.getResumeContent(), request.getJobDescription(), request.getSpecialRequirements());
            return ResponseEntity.ok(ApiResponse.success(result.getData()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/weekly-report")
    public ResponseEntity<ApiResponse<WeeklyReportResult>> generateWeeklyReport(@RequestBody WeeklyReportRequest request) {
        try {
            AgentResult<WeeklyReportResult> result = weeklyReportAgent.generate(
                    request.getJobsDiscovered(), request.getGreetingsSent(), request.getGreetingsReplied(),
                    request.getApplicationsSent(), request.getInterviewsScheduled(),
                    request.getComparisonWithLastWeek(), request.getSpecialEvents());
            return ResponseEntity.ok(ApiResponse.success(result.getData()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    // ========== Request DTOs ==========

    @lombok.Data
    public static class JdAnalyzeRequest {
        private String jobTitle;
        private String companyName;
        private String rawDescription;
    }

    @lombok.Data
    public static class MatchRequest {
        private String candidateName;
        private List<String> skills;
        private String experienceSummary;
        private Integer totalYears;
        private String jobTitle;
        private JdAnalysisResult jdAnalysis;
    }

    @lombok.Data
    public static class GreetRequest {
        private String candidateName;
        private List<String> topSkills;
        private String experienceHighlights;
        private Integer totalYears;
        private String jobTitle;
        private String companyName;
        private JdAnalysisResult jdAnalysis;
        private String greetingStyle;
    }

    @lombok.Data
    public static class ChatReplyRequest {
        private String hrMessage;
        private String conversationHistory;
        private String candidateName;
        private String resumeHighlights;
        private String jobPreference;
        private String expectedSalary;
        private String currentStatus;
    }

    @lombok.Data
    public static class RewriteRequest {
        private String resumeContent;
        private String jobDescription;
        private String specialRequirements;
    }

    @lombok.Data
    public static class WeeklyReportRequest {
        private Integer jobsDiscovered;
        private Integer greetingsSent;
        private Integer greetingsReplied;
        private Integer applicationsSent;
        private Integer interviewsScheduled;
        private String comparisonWithLastWeek;
        private String specialEvents;
    }
}
