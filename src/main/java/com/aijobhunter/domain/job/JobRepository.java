package com.aijobhunter.domain.job;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface JobRepository extends JpaRepository<Job, String> {

    List<Job> findByUserIdOrderByDiscoveredAtDesc(String userId);

    Optional<Job> findByIdAndUserId(String id, String userId);

    long countByUserId(String userId);

    long countByUserIdAndDiscoveredAtBetween(String userId, LocalDateTime start, LocalDateTime end);

    long countByUserIdAndStatusIn(String userId, Collection<Job.JobStatus> statuses);

    long countByUserIdAndStatusInAndUpdatedAtBetween(
            String userId, Collection<Job.JobStatus> statuses, LocalDateTime start, LocalDateTime end);

    long countByUserIdAndGreetingTextIsNotNull(String userId);

    long countByUserIdAndGreetingTextIsNotNullAndUpdatedAtBetween(
            String userId, LocalDateTime start, LocalDateTime end);
}
