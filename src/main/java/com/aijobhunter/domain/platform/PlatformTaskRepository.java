package com.aijobhunter.domain.platform;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlatformTaskRepository extends JpaRepository<PlatformTask, String> {

    List<PlatformTask> findByUserIdOrderByCreatedAtDesc(String userId);

    Optional<PlatformTask> findByIdAndUserId(String id, String userId);
}
