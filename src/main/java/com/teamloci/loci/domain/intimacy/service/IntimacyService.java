package com.teamloci.loci.domain.intimacy.service;

import com.teamloci.loci.domain.friend.Friendship;
import com.teamloci.loci.domain.friend.FriendshipRepository;
import com.teamloci.loci.domain.intimacy.dto.IntimacyDto;
import com.teamloci.loci.domain.intimacy.entity.FriendshipIntimacy;
import com.teamloci.loci.domain.intimacy.entity.IntimacyLevel;
import com.teamloci.loci.domain.intimacy.entity.IntimacyLog;
import com.teamloci.loci.domain.intimacy.entity.IntimacyType;
import com.teamloci.loci.domain.intimacy.event.IntimacyLevelUpEvent;
import com.teamloci.loci.domain.intimacy.event.NudgeEvent;
import com.teamloci.loci.domain.intimacy.repository.FriendshipIntimacyRepository;
import com.teamloci.loci.domain.intimacy.repository.IntimacyLevelRepository;
import com.teamloci.loci.domain.intimacy.repository.IntimacyLogRepository;
import com.teamloci.loci.domain.user.User;
import com.teamloci.loci.domain.user.UserDto;
import com.teamloci.loci.domain.user.UserRepository;
import com.teamloci.loci.domain.user.UserActivityService;
import com.teamloci.loci.global.error.CustomException;
import com.teamloci.loci.global.error.ErrorCode;
import com.teamloci.loci.global.util.RelationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class IntimacyService {

    private final FriendshipIntimacyRepository intimacyRepository;
    private final IntimacyLogRepository logRepository;
    private final IntimacyLevelRepository levelRepository;
    private final UserRepository userRepository;
    private final FriendshipRepository friendshipRepository;
    private final UserActivityService userActivityService;
    private final ApplicationEventPublisher eventPublisher;

    public void accumulatePoint(Long actorId, Long targetId, IntimacyType type, String relatedBeaconId) {
        if (actorId.equals(targetId)) return;

        if (isLimited(actorId, targetId, type, relatedBeaconId)) {
            return;
        }

        Long u1 = Math.min(actorId, targetId);
        Long u2 = Math.max(actorId, targetId);

        FriendshipIntimacy intimacy = intimacyRepository.findByUserAIdAndUserBIdWithLock(u1, u2)
                .orElseGet(() -> intimacyRepository.save(
                        FriendshipIntimacy.builder().user1Id(u1).user2Id(u2).build()
                ));

        int point = type.getPoint();
        int oldLevel = intimacy.getLevel();
        long newTotalScore = intimacy.getTotalScore() + point;

        int newLevel = calculateLevel(newTotalScore);

        intimacy.addScore(point);
        if (newLevel > oldLevel) {
            intimacy.updateLevel(newLevel);

            int levelDiff = newLevel - oldLevel;
            userRepository.increaseTotalIntimacy(actorId, levelDiff);
            userRepository.increaseTotalIntimacy(targetId, levelDiff);

            eventPublisher.publishEvent(new IntimacyLevelUpEvent(actorId, targetId, newLevel));
        }

        logRepository.save(IntimacyLog.builder()
                .actorId(actorId)
                .targetId(targetId)
                .type(type)
                .earnedPoint(point)
                .relatedBeaconId(relatedBeaconId)
                .build());

        if (type == IntimacyType.NUDGE) {
            User actor = userRepository.findById(actorId).orElseThrow();
            User target = userRepository.findById(targetId).orElseThrow();
            eventPublisher.publishEvent(new NudgeEvent(actor, target));
        }
    }

    @Transactional(readOnly = true)
    public Map<Long, FriendshipIntimacy> getIntimacyMap(Long myUserId, List<Long> targetUserIds) {
        if (targetUserIds == null || targetUserIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<FriendshipIntimacy> intimacies = intimacyRepository.findByUserIdAndTargetIdsIn(myUserId, targetUserIds);

        Map<Long, FriendshipIntimacy> map = new HashMap<>();
        for (FriendshipIntimacy fi : intimacies) {
            Long partnerId = fi.getUserAId().equals(myUserId) ? fi.getUserBId() : fi.getUserAId();
            map.put(partnerId, fi);
        }
        return map;
    }

    @Transactional(readOnly = true)
    public IntimacyDto.DetailResponse getIntimacyDetail(Long myUserId, Long targetUserId) {
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Long u1 = Math.min(myUserId, targetUserId);
        Long u2 = Math.max(myUserId, targetUserId);

        Optional<FriendshipIntimacy> intimacyOpt = intimacyRepository.findByUserAIdAndUserBId(u1, u2);

        int currentLevel = intimacyOpt.map(FriendshipIntimacy::getLevel).orElse(1);
        Long currentScore = intimacyOpt.map(FriendshipIntimacy::getTotalScore).orElse(0L);

        User me = userRepository.findById(myUserId).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        Integer myTotalLevel = me.getTotalIntimacyLevel();

        IntimacyLevel nextLevelInfo = levelRepository.findByLevel(currentLevel + 1).orElse(null);
        Long nextLevelScore = nextLevelInfo != null ? Long.valueOf(nextLevelInfo.getRequiredTotalScore()) : null;

        var stats = userActivityService.getUserStats(targetUserId);
        Optional<Friendship> friendship = friendshipRepository.findFriendshipBetween(myUserId, targetUserId);
        String relationStatus = RelationUtil.resolveStatus(friendship.orElse(null), myUserId);

        UserDto.UserResponse userResponse = UserDto.UserResponse.of(
                targetUser,
                relationStatus,
                stats.friendCount(),
                stats.postCount(),
                stats.streakCount(),
                stats.visitedPlaceCount()
        );
        userResponse.setTotalIntimacyLevel(stats.totalIntimacyLevel());

        return IntimacyDto.DetailResponse.builder()
                .targetUser(userResponse)
                .level(currentLevel)
                .score(currentScore)
                .nextLevelScore(nextLevelScore)
                .myTotalLevel(myTotalLevel)
                .build();
    }

    @Transactional(readOnly = true)
    public Map<Long, FriendshipIntimacy> getIntimacyMap(Long myUserId) {
        List<FriendshipIntimacy> intimacies = intimacyRepository.findAllByUserId(myUserId);
        Map<Long, FriendshipIntimacy> map = new HashMap<>();
        for (FriendshipIntimacy fi : intimacies) {
            Long partnerId = fi.getUserAId().equals(myUserId) ? fi.getUserBId() : fi.getUserAId();
            map.put(partnerId, fi);
        }
        return map;
    }

    private int calculateLevel(long currentScore) {
        List<IntimacyLevel> levels = levelRepository.findAllByOrderByLevelDesc();

        for (IntimacyLevel lv : levels) {
            if (currentScore >= lv.getRequiredTotalScore()) {
                return lv.getLevel();
            }
        }
        return 1;
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

    public int getIntimacyLevel(Long userId1, Long userId2) {
        if (userId1.equals(userId2)) return 0;

        Long u1 = Math.min(userId1, userId2);
        Long u2 = Math.max(userId1, userId2);

        return intimacyRepository.findByUserAIdAndUserBId(u1, u2)
                .map(FriendshipIntimacy::getLevel)
                .orElse(0);
    }
}