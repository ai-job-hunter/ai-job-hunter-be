package com.aijobhunter.service;

import com.aijobhunter.agent.GreetAgent;
import com.aijobhunter.agent.JdAnalyzerAgent;
import com.aijobhunter.agent.MatchAgent;
import com.aijobhunter.agent.model.JdAnalysisResult;
import com.aijobhunter.agent.AgentResult;
import com.aijobhunter.domain.job.Job;
import com.aijobhunter.domain.job.JobRepository;
import com.aijobhunter.domain.resume.ResumeRepository;
import com.aijobhunter.dto.CreateJobRequest;
import com.aijobhunter.dto.JobDTO;
import com.aijobhunter.exception.AgentException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * JobService 单元测试：不依赖 DB / LLM，覆盖粘贴 JD 核心分支。
 */
@ExtendWith(MockitoExtension.class)
class JobServiceTest {

    @Mock JobRepository jobRepository;
    @Mock ResumeRepository resumeRepository;
    @Mock JdAnalyzerAgent jdAnalyzerAgent;
    @Mock MatchAgent matchAgent;
    @Mock GreetAgent greetAgent;

    private JobService jobService;

    @BeforeEach
    void setUp() {
        jobService = new JobService(
                jobRepository,
                resumeRepository,
                jdAnalyzerAgent,
                matchAgent,
                greetAgent,
                new ObjectMapper());
    }

    @Test
    @DisplayName("空 JD 应拒绝创建")
    void createRejectsBlankJd() {
        CreateJobRequest req = new CreateJobRequest();
        req.setRawDescription("  ");
        assertThatThrownBy(() -> jobService.create("u1", req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("JD");
    }

    @Test
    @DisplayName("无标题时用 JD 首行；analyze=false 不调 AI")
    void createWithoutAnalyzeUsesFirstLineAsTitle() {
        when(jobRepository.save(any(Job.class))).thenAnswer(inv -> {
            Job j = inv.getArgument(0);
            j.setId("job-1");
            return j;
        });

        CreateJobRequest req = new CreateJobRequest();
        req.setRawDescription("Java工程师\n熟悉 Spring Boot");
        req.setAnalyze(false);

        JobDTO dto = jobService.create("u1", req);
        assertThat(dto.getTitle()).isEqualTo("Java工程师");
        assertThat(dto.getId()).isEqualTo("job-1");
        assertThat(dto.getStatus()).isEqualTo("NEW");

        ArgumentCaptor<Job> cap = ArgumentCaptor.forClass(Job.class);
        verify(jobRepository).save(cap.capture());
        assertThat(cap.getValue().getSource()).isEqualTo("MANUAL");
        assertThat(cap.getValue().getUserId()).isEqualTo("u1");
    }

    @Test
    @DisplayName("AI 失败时降级分析仍保存并标记 ANALYZED")
    void createAnalyzeDegradesWhenAgentFails() {
        when(jobRepository.save(any(Job.class))).thenAnswer(inv -> {
            Job j = inv.getArgument(0);
            if (j.getId() == null) {
                j.setId("job-2");
            }
            return j;
        });
        when(jdAnalyzerAgent.analyze(anyString(), anyString(), anyString()))
                .thenThrow(new AgentException("JD_ANALYZER", "llm down"));
        when(jdAnalyzerAgent.degradeAnalyze(anyString())).thenReturn(
                JdAnalysisResult.builder()
                        .skills(List.of())
                        .mustHaveSkills(List.of())
                        .redFlags(List.of("（降级模式）"))
                        .build());

        CreateJobRequest req = new CreateJobRequest();
        req.setTitle("后端");
        req.setCompanyName("测试司");
        req.setRawDescription("需要 Java Spring Boot MySQL 经验");
        req.setAnalyze(true);

        JobDTO dto = jobService.create("u1", req);
        assertThat(dto.getStatus()).isEqualTo("ANALYZED");
        assertThat(dto.getAnalysis()).isNotNull();
        verify(jdAnalyzerAgent).degradeAnalyze(anyString());
    }

    @Test
    @DisplayName("AI 成功时写入 analysis 与 keywords")
    void createAnalyzeSuccess() {
        when(jobRepository.save(any(Job.class))).thenAnswer(inv -> {
            Job j = inv.getArgument(0);
            if (j.getId() == null) {
                j.setId("job-3");
            }
            return j;
        });
        JdAnalysisResult analysis = JdAnalysisResult.builder()
                .skills(List.of("Java", "Spring Boot"))
                .mustHaveSkills(List.of("Java"))
                .niceToHaveSkills(List.of("Redis"))
                .salaryRange("20k-35k")
                .build();
        when(jdAnalyzerAgent.analyze(anyString(), anyString(), anyString()))
                .thenReturn(AgentResult.<JdAnalysisResult>builder().success(true).data(analysis).build());

        CreateJobRequest req = new CreateJobRequest();
        req.setTitle("Java");
        req.setRawDescription("JD body");
        req.setAnalyze(true);

        JobDTO dto = jobService.create("u1", req);
        assertThat(dto.getStatus()).isEqualTo("ANALYZED");
        assertThat(dto.getKeywords()).contains("Java", "Spring Boot");
        assertThat(dto.getSalaryText()).isEqualTo("20k-35k");
        assertThat(dto.getAnalysis()).containsKey("skills");
    }
}
