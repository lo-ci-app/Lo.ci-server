package com.teamloci.loci.domain.intimacy.repository;

import com.teamloci.loci.domain.intimacy.entity.IntimacyLog;
import com.teamloci.loci.domain.intimacy.entity.IntimacyType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface IntimacyLogRepository extends JpaRepository<IntimacyLog, Long> {

    boolean existsByActorIdAndTargetIdAndTypeAndCreatedAtBetween(
            Long actorId, Long targetId, IntimacyType type, LocalDateTime start, LocalDateTime end);

    @Query("SELECT COUNT(l) > 0 FROM IntimacyLog l " +
            "WHERE l.actorId = :actorId AND l.targetId = :targetId " +
            "AND l.type = :type AND l.relatedBeaconId = :beaconId " +
            "AND l.createdAt >= :since")
    boolean existsVisitLogRecent(
            @Param("actorId") Long actorId,
            @Param("targetId") Long targetId,
            @Param("type") IntimacyType type,
            @Param("beaconId") String beaconId,
            @Param("since") LocalDateTime since);

    boolean existsByActorIdAndTargetIdAndType(Long actorId, Long targetId, IntimacyType type);

    @Query("SELECT COUNT(l) > 0 FROM IntimacyLog l " +
            "WHERE l.type = :type " +
            "AND ( " +
            "   (l.actorId = :user1Id AND l.targetId = :user2Id) " +
            "   OR " +
            "   (l.actorId = :user2Id AND l.targetId = :user1Id) " +
            ")")
    boolean existsFriendMadeLogBetween(@Param("user1Id") Long user1Id, @Param("user2Id") Long user2Id, @Param("type") IntimacyType type);

    long countByTargetIdAndType(Long targetId, IntimacyType type);
}