package com.aijobhunter.domain.application;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, String> {

    List<Application> findByUserIdOrderByAppliedAtDesc(String userId);

    Optional<Application> findByIdAndUserId(String id, String userId);

    long countByUserId(String userId);

    boolean existsByUserIdAndJobId(String userId, String jobId);

    long countByUserIdAndAppliedAtBetween(String userId, LocalDateTime start, LocalDateTime end);

    long countByUserIdAndStatusIn(String userId, Collection<Application.ApplicationStatus> statuses);

    long countByUserIdAndStatusInAndAppliedAtBetween(
            String userId, Collection<Application.ApplicationStatus> statuses,
            LocalDateTime start, LocalDateTime end);

    long countByUserIdAndStatusInAndUpdatedAtBetween(
            String userId, Collection<Application.ApplicationStatus> statuses,
            LocalDateTime start, LocalDateTime end);
}
