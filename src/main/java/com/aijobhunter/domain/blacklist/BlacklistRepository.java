package com.aijobhunter.domain.blacklist;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BlacklistRepository extends JpaRepository<Blacklist, String> {

    List<Blacklist> findByUserIdOrderByAddedAtDesc(String userId);

    Optional<Blacklist> findByIdAndUserId(String id, String userId);

    boolean existsByUserIdAndTypeAndValueIgnoreCase(String userId, Blacklist.BlacklistType type, String value);
}
