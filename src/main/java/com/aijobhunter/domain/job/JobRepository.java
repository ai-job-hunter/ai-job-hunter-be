package com.aijobhunter.domain.job;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JobRepository extends JpaRepository<Job, String> {

    List<Job> findByUserIdOrderByDiscoveredAtDesc(String userId);

    Optional<Job> findByIdAndUserId(String id, String userId);

    long countByUserId(String userId);
}
