package com.aijobhunter.domain.conversation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, String> {

    List<Conversation> findByUserIdOrderByLastMessageAtDesc(String userId);

    Optional<Conversation> findByIdAndUserId(String id, String userId);

    long countByUserId(String userId);

    long countByUserIdAndCreatedAtBetween(String userId, LocalDateTime start, LocalDateTime end);
}
