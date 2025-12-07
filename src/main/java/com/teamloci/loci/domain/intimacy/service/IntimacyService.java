package com.teamloci.loci.domain.intimacy.service;

import com.teamloci.loci.domain.friend.Friendship;
import com.teamloci.loci.domain.friend.FriendshipRepository;
import com.teamloci.loci.domain.intimacy.dto.IntimacyDto;
import com.teamloci.loci.domain.intimacy.entity.FriendshipIntimacy;
import com.teamloci.loci.domain.intimacy.entity.IntimacyLog;
import com.teamloci.loci.domain.intimacy.entity.IntimacyType;
import com.teamloci.loci.domain.intimacy.repository.FriendshipIntimacyRepository;
import com.teamloci.loci.domain.intimacy.repository.IntimacyLogRepository;
import com.teamloci.loci.domain.user.User;
import com.teamloci.loci.domain.user.UserDto;
import com.teamloci.loci.domain.user.UserRepository;
import com.teamloci.loci.domain.user.service.UserActivityService;
import com.teamloci.loci.global.error.CustomException;
import com.teamloci.loci.global.error.ErrorCode;
import com.teamloci.loci.global.util.RelationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class IntimacyService {

    private final FriendshipIntimacyRepository intimacyRepository;
    private final IntimacyLogRepository logRepository;
    private final UserRepository userRepository;
    private final FriendshipRepository friendshipRepository;
    private final UserActivityService userActivityService;

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
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);

        switch (type) {
            case FRIEND_MADE:
                return logRepository.existsFriendMadeLogBetween(actorId, targetId, type);

            case REACTION:
            case COMMENT:
            case COLLABORATOR:
                return logRepository.existsByActorIdAndTargetIdAndTypeAndCreatedAtBetween(
                        actorId, targetId, type, startOfDay, endOfDay);

            case VISIT:
                LocalDateTime oneWeekAgo = LocalDateTime.now().minusDays(7);
                return logRepository.existsVisitLogRecent(actorId, targetId, type, beaconId, oneWeekAgo);

            default:
                return false;
        }
    }

    @Transactional(readOnly = true)
    public IntimacyDto.DetailResponse getIntimacyDetail(Long myUserId, Long targetUserId) {
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 1. 친밀도 조회 (없으면 기본값 0점, Lv.1)
        Long u1 = Math.min(myUserId, targetUserId);
        Long u2 = Math.max(myUserId, targetUserId);

        Optional<FriendshipIntimacy> intimacyOpt = intimacyRepository.findByUserAIdAndUserBId(u1, u2);

        int level = intimacyOpt.map(FriendshipIntimacy::getLevel).orElse(1);
        Long score = intimacyOpt.map(FriendshipIntimacy::getTotalScore).orElse(0L);

        Optional<Friendship> friendship = friendshipRepository.findFriendshipBetween(myUserId, targetUserId);
        String relationStatus = RelationUtil.resolveStatus(friendship.orElse(null), myUserId);

        var stats = userActivityService.getUserStats(targetUserId);

        UserDto.UserResponse userResponse = UserDto.UserResponse.of(
                targetUser,
                relationStatus,
                stats.friendCount(),
                stats.postCount(),
                stats.streakCount(),
                stats.visitedPlaceCount()
        );

        return IntimacyDto.DetailResponse.builder()
                .targetUser(userResponse)
                .level(level)
                .score(score)
                .build();
    }
}