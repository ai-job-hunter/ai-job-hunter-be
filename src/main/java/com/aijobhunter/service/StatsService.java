package com.aijobhunter.service;

import com.aijobhunter.agent.WeeklyReportAgent;
import com.aijobhunter.agent.model.WeeklyReportResult;
import com.aijobhunter.domain.application.Application;
import com.aijobhunter.domain.application.ApplicationRepository;
import com.aijobhunter.domain.conversation.ConversationRepository;
import com.aijobhunter.domain.job.Job;
import com.aijobhunter.domain.job.JobRepository;
import com.aijobhunter.domain.resume.ResumeRepository;
import com.aijobhunter.exception.AgentException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * V1 统计：从岗位 / 对话 / 投递实时聚合，不落 daily_stats 表。
 * 打招呼 ≈ 已生成 greetingText 的岗位；回复 ≈ 投递状态 REPLIED+ 或有对话。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StatsService {

    private static final Set<Job.JobStatus> GREETED_STATUSES = EnumSet.of(
            Job.JobStatus.GREETED, Job.JobStatus.REPLIED, Job.JobStatus.APPLIED,
            Job.JobStatus.INTERVIEWING, Job.JobStatus.OFFER);

    private static final Set<Application.ApplicationStatus> REPLIED_PLUS = EnumSet.of(
            Application.ApplicationStatus.REPLIED,
            Application.ApplicationStatus.INTERVIEWING,
            Application.ApplicationStatus.OFFERED);

    private static final Set<Application.ApplicationStatus> INTERVIEW_PLUS = EnumSet.of(
            Application.ApplicationStatus.INTERVIEWING,
            Application.ApplicationStatus.OFFERED);

    private static final Set<Application.ApplicationStatus> OFFERED = EnumSet.of(
            Application.ApplicationStatus.OFFERED);

    private final JobRepository jobRepository;
    private final ApplicationRepository applicationRepository;
    private final ConversationRepository conversationRepository;
    private final ResumeRepository resumeRepository;
    private final WeeklyReportAgent weeklyReportAgent;

    @Transactional(readOnly = true)
    public Map<String, Object> today(String userId) {
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        Map<String, Object> t = daySnapshot(userId, today);
        Map<String, Object> y = daySnapshot(userId, yesterday);

        Map<String, Object> compared = new LinkedHashMap<>();
        compared.put("jobsDiscovered", delta(t, y, "jobsDiscovered"));
        compared.put("greetingsSent", delta(t, y, "greetingsSent"));
        compared.put("applicationsSent", delta(t, y, "applicationsSent"));

        Map<String, Object> data = new LinkedHashMap<>(t);
        data.put("date", today.toString());
        data.put("comparedToYesterday", compared);
        data.put("tokensUsed", 0);
        return data;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> trend(String userId, String period) {
        int days = switch (period == null ? "week" : period.toLowerCase(Locale.ROOT)) {
            case "month" -> 30;
            case "quarter" -> 90;
            default -> 7;
        };
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(days - 1L);

        List<Map<String, Object>> points = new ArrayList<>();
        int totalGreetings = 0;
        int totalApplications = 0;
        double replySum = 0;
        int replyDays = 0;

        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            Map<String, Object> snap = daySnapshot(userId, d);
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("date", d.toString());
            point.put("jobsDiscovered", snap.get("jobsDiscovered"));
            point.put("greetingsSent", snap.get("greetingsSent"));
            point.put("applicationsSent", snap.get("applicationsSent"));
            point.put("replyRate", snap.get("replyRate"));
            points.add(point);

            totalGreetings += ((Number) snap.get("greetingsSent")).intValue();
            totalApplications += ((Number) snap.get("applicationsSent")).intValue();
            double rr = ((Number) snap.get("replyRate")).doubleValue();
            if (((Number) snap.get("greetingsSent")).intValue() > 0) {
                replySum += rr;
                replyDays++;
            }
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalGreetings", totalGreetings);
        summary.put("totalApplications", totalApplications);
        summary.put("avgReplyRate", replyDays == 0 ? 0.0 : round1(replySum / replyDays));
        summary.put("avgInterviewRate", 0.0);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("period", period == null || period.isBlank() ? "week" : period.toLowerCase(Locale.ROOT));
        data.put("startDate", start.toString());
        data.put("endDate", end.toString());
        data.put("dataPoints", points);
        data.put("summary", summary);
        return data;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> funnel(String userId) {
        long jobs = jobRepository.countByUserId(userId);
        long greetings = Math.max(
                jobRepository.countByUserIdAndGreetingTextIsNotNull(userId),
                jobRepository.countByUserIdAndStatusIn(userId, GREETED_STATUSES));
        long conversations = conversationRepository.countByUserId(userId);
        long replies = Math.max(
                applicationRepository.countByUserIdAndStatusIn(userId, REPLIED_PLUS),
                conversations);
        long applications = applicationRepository.countByUserId(userId);
        long interviews = applicationRepository.countByUserIdAndStatusIn(userId, INTERVIEW_PLUS);
        long offers = applicationRepository.countByUserIdAndStatusIn(userId, OFFERED);

        long base = Math.max(jobs, 1);
        List<Map<String, Object>> stages = List.of(
                stage("发现岗位", jobs, base),
                stage("打招呼", greetings, base),
                stage("HR回复", replies, base),
                stage("投递简历", applications, base),
                stage("进入面试", interviews, base),
                stage("拿到Offer", offers, base));

        Map<String, Object> rates = new LinkedHashMap<>();
        rates.put("greetToReply", pct(replies, greetings));
        rates.put("replyToApply", pct(applications, replies));
        rates.put("applyToInterview", pct(interviews, applications));
        rates.put("interviewToOffer", pct(offers, interviews));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("totalJobsDiscovered", jobs);
        data.put("totalGreetingsSent", greetings);
        data.put("totalReplies", replies);
        data.put("totalApplications", applications);
        data.put("totalInterviews", interviews);
        data.put("totalOffers", offers);
        data.put("resumeCount", resumeRepository.countByUserId(userId));
        data.put("conversationCount", conversations);
        data.put("funnel", stages);
        data.put("conversionRates", rates);
        return data;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> weeklyReport(String userId, LocalDate weekStartInput) {
        LocalDate weekStart = weekStartInput != null
                ? weekStartInput.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                : LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = weekStart.plusDays(6);
        LocalDateTime start = weekStart.atStartOfDay();
        LocalDateTime end = weekEnd.atTime(LocalTime.MAX);

        int jobs = (int) jobRepository.countByUserIdAndDiscoveredAtBetween(userId, start, end);
        int greetings = (int) Math.max(
                jobRepository.countByUserIdAndGreetingTextIsNotNullAndUpdatedAtBetween(userId, start, end),
                jobRepository.countByUserIdAndStatusInAndUpdatedAtBetween(userId, GREETED_STATUSES, start, end));
        int applications = (int) applicationRepository.countByUserIdAndAppliedAtBetween(userId, start, end);
        int interviews = (int) applicationRepository.countByUserIdAndStatusInAndUpdatedAtBetween(
                userId, INTERVIEW_PLUS, start, end);
        int replies = (int) Math.max(
                applicationRepository.countByUserIdAndStatusInAndUpdatedAtBetween(userId, REPLIED_PLUS, start, end),
                conversationRepository.countByUserIdAndCreatedAtBetween(userId, start, end));

        LocalDate prevStart = weekStart.minusWeeks(1);
        LocalDate prevEnd = weekStart.minusDays(1);
        LocalDateTime pStart = prevStart.atStartOfDay();
        LocalDateTime pEnd = prevEnd.atTime(LocalTime.MAX);
        int prevApps = (int) applicationRepository.countByUserIdAndAppliedAtBetween(userId, pStart, pEnd);
        int prevGreet = (int) jobRepository.countByUserIdAndGreetingTextIsNotNullAndUpdatedAtBetween(
                userId, pStart, pEnd);
        String comparison = String.format("上周打招呼 %d，投递 %d；本周打招呼 %d，投递 %d",
                prevGreet, prevApps, greetings, applications);

        WeeklyReportResult report;
        try {
            var agentResult = weeklyReportAgent.generate(
                    jobs, greetings, replies, applications, interviews, comparison, null);
            report = agentResult != null && agentResult.getData() != null
                    ? agentResult.getData()
                    : weeklyReportAgent.degradeGenerate(greetings, replies, applications, interviews);
        } catch (AgentException e) {
            log.warn("WeeklyReportAgent failed, degrade: {}", e.getMessage());
            report = weeklyReportAgent.degradeGenerate(greetings, replies, applications, interviews);
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("jobsDiscovered", jobs);
        summary.put("greetingsSent", greetings);
        summary.put("repliesReceived", replies);
        summary.put("applicationsSent", applications);
        summary.put("interviewsScheduled", interviews);
        summary.put("replyRate", pct(replies, greetings));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("weekStartDate", weekStart.toString());
        data.put("weekEndDate", weekEnd.toString());
        data.put("summary", summary);
        data.put("title", report.getTitle());
        data.put("content", report.getContent());
        data.put("overview", report.getOverview());
        data.put("nextWeekSuggestions", report.getNextWeekSuggestions());
        return data;
    }

    private Map<String, Object> daySnapshot(String userId, LocalDate day) {
        LocalDateTime start = day.atStartOfDay();
        LocalDateTime end = day.atTime(LocalTime.MAX);

        int jobs = (int) jobRepository.countByUserIdAndDiscoveredAtBetween(userId, start, end);
        int greetings = (int) Math.max(
                jobRepository.countByUserIdAndGreetingTextIsNotNullAndUpdatedAtBetween(userId, start, end),
                jobRepository.countByUserIdAndStatusInAndUpdatedAtBetween(userId, GREETED_STATUSES, start, end));
        int applications = (int) applicationRepository.countByUserIdAndAppliedAtBetween(userId, start, end);
        int interviews = (int) applicationRepository.countByUserIdAndStatusInAndUpdatedAtBetween(
                userId, INTERVIEW_PLUS, start, end);
        int replies = (int) Math.max(
                applicationRepository.countByUserIdAndStatusInAndUpdatedAtBetween(userId, REPLIED_PLUS, start, end),
                conversationRepository.countByUserIdAndCreatedAtBetween(userId, start, end));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("jobsDiscovered", jobs);
        data.put("greetingsSent", greetings);
        data.put("greetingsReplied", replies);
        data.put("applicationsSent", applications);
        data.put("interviewsScheduled", interviews);
        data.put("replyRate", pct(replies, greetings));
        data.put("conversionRate", pct(applications, Math.max(greetings, replies)));
        return data;
    }

    private static Map<String, Object> stage(String name, long count, long base) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("stage", name);
        m.put("count", count);
        m.put("percentage", round1(count * 100.0 / base));
        return m;
    }

    private static int delta(Map<String, Object> a, Map<String, Object> b, String key) {
        return ((Number) a.get(key)).intValue() - ((Number) b.get(key)).intValue();
    }

    private static double pct(long num, long den) {
        if (den <= 0) return 0.0;
        return round1(num * 100.0 / den);
    }

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
