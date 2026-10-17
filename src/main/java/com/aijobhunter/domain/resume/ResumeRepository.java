package com.aijobhunter.domain.resume;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResumeRepository extends JpaRepository<Resume, String> {

    List<Resume> findByUserIdOrderByCreatedAtDesc(String userId);

    List<Resume> findByUserIdAndTypeOrderByCreatedAtDesc(String userId, Resume.ResumeType type);

    Optional<Resume> findByIdAndUserId(String id, String userId);

    long countByUserId(String userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Resume r set r.isDefault = false where r.userId = :userId and r.isDefault = true")
    void clearDefaultForUser(@Param("userId") String userId);
}
