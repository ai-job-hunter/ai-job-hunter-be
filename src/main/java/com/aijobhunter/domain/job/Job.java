package com.aijobhunter.domain.job;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "jobs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(length = 20)
    @Builder.Default
    private String source = "MANUAL";

    @Column(name = "source_url", columnDefinition = "text")
    private String sourceUrl;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "company_name", length = 200)
    private String companyName;

    @Column(length = 50)
    private String city;

    @Column(name = "salary_text", length = 50)
    private String salaryText;

    @Column(name = "raw_description", columnDefinition = "text")
    private String rawDescription;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> analysis;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> keywords;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "match_scores", columnDefinition = "jsonb")
    private Map<String, Object> matchScores;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    @Builder.Default
    private JobStatus status = JobStatus.NEW;

    @Column(name = "greeting_text", columnDefinition = "text")
    private String greetingText;

    @Column(name = "discovered_at")
    private LocalDateTime discoveredAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (discoveredAt == null) {
            discoveredAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum JobStatus {
        NEW,
        ANALYZED,
        GREETED,
        REPLIED,
        APPLIED,
        INTERVIEWING,
        OFFER,
        REJECTED,
        EXPIRED,
        BLACKLISTED
    }
}
