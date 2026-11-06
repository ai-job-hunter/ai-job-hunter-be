package com.aijobhunter.domain.application;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "applications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "job_id")
    private String jobId;

    @Column(name = "conversation_id")
    private String conversationId;

    @Column(name = "resume_id")
    private String resumeId;

    @Column(name = "applied_at")
    private LocalDateTime appliedAt;

    @Column(name = "apply_note", columnDefinition = "text")
    private String applyNote;

    @Column(name = "match_score")
    private Integer matchScore;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    @Builder.Default
    private ApplicationStatus status = ApplicationStatus.SUBMITTED;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (appliedAt == null) {
            appliedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum ApplicationStatus {
        SUBMITTED,
        VIEWED,
        REPLIED,
        INTERVIEWING,
        OFFERED,
        REJECTED,
        WITHDRAWN
    }
}
