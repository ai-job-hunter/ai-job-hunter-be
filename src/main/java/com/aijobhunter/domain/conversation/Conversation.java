package com.aijobhunter.domain.conversation;

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
@Table(name = "conversations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "job_id")
    private String jobId;

    @Column(name = "hr_name", length = 100)
    private String hrName;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    @Builder.Default
    private ConversationStatus status = ConversationStatus.CHATTING;

    @Column(name = "total_messages")
    @Builder.Default
    private Integer totalMessages = 0;

    @Column(name = "unread_count")
    @Builder.Default
    private Integer unreadCount = 0;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "last_message", columnDefinition = "jsonb")
    private Map<String, Object> lastMessage;

    @Column(name = "greeting_sent")
    @Builder.Default
    private Boolean greetingSent = false;

    @Column(name = "greeting_text", columnDefinition = "text")
    private String greetingText;

    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum ConversationStatus {
        GREETING, CHATTING, WAITING, CLOSED
    }
}
