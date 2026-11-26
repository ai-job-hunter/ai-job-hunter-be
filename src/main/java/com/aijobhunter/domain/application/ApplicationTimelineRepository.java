package com.aijobhunter.domain.application;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApplicationTimelineRepository extends JpaRepository<ApplicationTimeline, String> {

    List<ApplicationTimeline> findByApplicationIdOrderByOccurredAtAscCreatedAtAsc(String applicationId);
}
