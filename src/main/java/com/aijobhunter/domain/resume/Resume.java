package com.aijobhunter.domain.resume;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "resumes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Resume {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private ResumeType type = ResumeType.GENERAL;

    @Column(name = "is_default")
    @Builder.Default
    private Boolean isDefault = false;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "parsed_data", columnDefinition = "jsonb")
    private Map<String, Object> parsedData;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "file_info", columnDefinition = "jsonb")
    private Map<String, Object> fileInfo;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> analysis;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private ResumeStatus status = ResumeStatus.UPLOADED;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum ResumeType {
        GENERAL, TECHNICAL, MANAGEMENT, CUSTOM
    }

    public enum ResumeStatus {
        UPLOADED, PARSED, ANALYZED, FAILED
    }
}
