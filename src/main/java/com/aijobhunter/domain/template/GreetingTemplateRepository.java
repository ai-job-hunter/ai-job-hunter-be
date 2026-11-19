package com.aijobhunter.domain.template;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GreetingTemplateRepository extends JpaRepository<GreetingTemplate, String> {

    List<GreetingTemplate> findByUserIdOrderByCreatedAtDesc(String userId);

    Optional<GreetingTemplate> findByIdAndUserId(String id, String userId);

    Optional<GreetingTemplate> findFirstByUserIdAndIsDefaultTrueAndIsActiveTrue(String userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update GreetingTemplate t set t.isDefault = false where t.userId = :userId and t.isDefault = true")
    void clearDefaultForUser(@Param("userId") String userId);
}
