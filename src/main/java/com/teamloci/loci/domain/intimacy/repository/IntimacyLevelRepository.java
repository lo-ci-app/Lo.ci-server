package com.teamloci.loci.domain.intimacy.repository;

import com.teamloci.loci.domain.intimacy.entity.IntimacyLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface IntimacyLevelRepository extends JpaRepository<IntimacyLevel, Integer> {
    List<IntimacyLevel> findAllByOrderByLevelDesc();

    Optional<IntimacyLevel> findByLevel(Integer level);

    void deleteByActorIdOrTargetId(Long actorId, Long targetId);
}