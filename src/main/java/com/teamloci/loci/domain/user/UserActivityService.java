package com.teamloci.loci.domain.user;

import com.teamloci.loci.domain.post.repository.PostRepository;
import com.teamloci.loci.global.error.CustomException;
import com.teamloci.loci.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserActivityService {

    private final UserRepository userRepository;
    private final PostRepository postRepository;

    public record UserStats(long friendCount, long postCount, long streakCount, long visitedPlaceCount, int totalIntimacyLevel) {}


    @Cacheable(value = "userStats", key = "#userId")
    public UserStats getUserStats(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return new UserStats(0, 0, 0, 0, 0);
        }

        long effectiveStreak = calculateEffectiveStreak(user);

        return new UserStats(
                user.getFriendCount(),
                user.getPostCount(),
                effectiveStreak,
                user.getVisitedPlaceCount(),
                user.getTotalIntimacyLevel()
        );
    }

    public Map<Long, UserStats> getUserStatsMap(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<User> users = userRepository.findAllById(userIds);

        return users.stream().collect(Collectors.toMap(
                User::getId,
                u -> {
                    long effectiveStreak = calculateEffectiveStreak(u);

                    return new UserStats(
                            u.getFriendCount(),
                            u.getPostCount(),
                            effectiveStreak,
                            u.getVisitedPlaceCount(),
                            u.getTotalIntimacyLevel()
                    );
                }
        ));
    }

    private long calculateEffectiveStreak(User user) {
        long currentStreak = user.getStreakCount();
        if (currentStreak == 0) {
            return 0;
        }

        LocalDate lastPostDate = user.getLastPostDate();
        if (lastPostDate == null) {
            return 0;
        }

        ZoneId zoneId = user.getZoneIdOrDefault();
        LocalDate today = LocalDate.now(zoneId);
        LocalDate yesterday = today.minusDays(1);

        if (lastPostDate.isBefore(yesterday)) {
            return 0;
        }

        return currentStreak;
    }

    @Transactional
    public void updateUserStats(Long userId, String beaconId) {
        try {
            User user = userRepository.findByIdWithLock(userId)
                    .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

            user.increasePostCount();

            ZoneId userZone = user.getZoneIdOrDefault();
            LocalDate today = LocalDate.now(userZone);

            LocalDate lastPostDate = user.getLastPostDate();
            long currentStreak = user.getStreakCount();

            if (lastPostDate == null) {
                user.updateStreakInfo(1L, today);
            } else if (lastPostDate.equals(today)) {
            } else if (lastPostDate.equals(today.minusDays(1))) {
                user.updateStreakInfo(currentStreak + 1, today);
            } else {
                user.updateStreakInfo(1L, today);
            }

            long existingPostsInBeacon = postRepository.countByUserIdAndBeaconId(userId, beaconId);
            if (existingPostsInBeacon == 1) {
                user.increaseVisitedPlaceCount();
            }

        } catch (Exception e) {
            log.error("유저 통계 업데이트 실패: {}", e.getMessage());
            throw e;
        }
    }

    @Transactional
    @CacheEvict(value = "userStats", key = "#userId")
    public void decreaseUserStats(Long userId, long remainingPosts) {
        User user = userRepository.findByIdWithLock(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        user.decreasePostCount();

        if (remainingPosts == 0) {
            user.decreaseVisitedPlaceCount();
        }
    }
}