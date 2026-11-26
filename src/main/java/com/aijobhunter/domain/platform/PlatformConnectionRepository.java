package com.aijobhunter.domain.platform;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlatformConnectionRepository extends JpaRepository<PlatformConnection, String> {

    List<PlatformConnection> findByUserIdOrderByCreatedAtDesc(String userId);

    Optional<PlatformConnection> findByIdAndUserId(String id, String userId);

    Optional<PlatformConnection> findByUserIdAndPlatform(String userId, String platform);
}
