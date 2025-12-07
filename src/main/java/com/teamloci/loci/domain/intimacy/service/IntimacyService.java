package com.teamloci.loci.domain.intimacy.service;

import com.teamloci.loci.domain.intimacy.entity.FriendshipIntimacy;
import com.teamloci.loci.domain.intimacy.entity.IntimacyLog;
import com.teamloci.loci.domain.intimacy.entity.IntimacyType;
import com.teamloci.loci.domain.intimacy.repository.FriendshipIntimacyRepository;
import com.teamloci.loci.domain.intimacy.repository.IntimacyLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class IntimacyService {

    private final FriendshipIntimacyRepository intimacyRepository;
    private final IntimacyLogRepository logRepository;

    public void accumulatePoint(Long actorId, Long targetId, IntimacyType type, String relatedBeaconId) {
        if (actorId.equals(targetId)) return;

        if (isLimited(actorId, targetId, type, relatedBeaconId)) {
            return;
        }

        Long u1 = Math.min(actorId, targetId);
        Long u2 = Math.max(actorId, targetId);

        FriendshipIntimacy intimacy = intimacyRepository.findByUserAIdAndUserBId(u1, u2)
                .orElseGet(() -> intimacyRepository.save(
                        FriendshipIntimacy.builder().user1Id(u1).user2Id(u2).build()
                ));

        int point = type.getPoint();
        int oldLevel = intimacy.getLevel();

        intimacy.addScore(point);

        if (intimacy.getLevel() > oldLevel) {
            log.info("🎉 레벨업! {} & {} -> Lv.{}", actorId, targetId, intimacy.getLevel());
        }

        logRepository.save(IntimacyLog.builder()
                .actorId(actorId)
                .targetId(targetId)
                .type(type)
                .earnedPoint(point)
                .relatedBeaconId(relatedBeaconId)
                .build());
    }

    private boolean isLimited(Long actorId, Long targetId, IntimacyType type, String beaconId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.plusDays(1).atStartOfDay();

        switch (type) {
            case FRIEND_MADE:
                return logRepository.existsByActorIdAndTargetIdAndType(actorId, targetId, type) ||
                        logRepository.existsByActorIdAndTargetIdAndType(targetId, actorId, type);

            case REACTION:
            case COMMENT:
            case COLLABORATOR:
                return logRepository.existsByActorIdAndTargetIdAndTypeAndCreatedAtBetween(
                        actorId, targetId, type, startOfDay, endOfDay);

            case VISIT:
                LocalDateTime oneWeekAgo = now.minusDays(7);
                return logRepository.existsVisitLogRecent(actorId, targetId, type, beaconId, oneWeekAgo);

            default:
                return false;
        }
    }
}