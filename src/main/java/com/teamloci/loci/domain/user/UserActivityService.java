package com.teamloci.loci.domain.user;

import com.teamloci.loci.domain.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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
        return new UserStats(
                user.getFriendCount(),
                user.getPostCount(),
                user.getStreakCount(),
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
                u -> new UserStats(
                        u.getFriendCount(),
                        u.getPostCount(),
                        u.getStreakCount(),
                        u.getVisitedPlaceCount(),
                        u.getTotalIntimacyLevel()
                )
        ));
    }

    @Transactional
    public void updateUserStats(Long userId, String beaconId) {
        try {
            userRepository.increasePostCount(userId);

            // 비관적 락 사용 (경쟁 조건 방지)
            Optional<User> userOpt = userRepository.findByIdWithLock(userId);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                LocalDate today = LocalDate.now();
                LocalDate lastPostDate = user.getLastPostDate();
                long currentStreak = user.getStreakCount();

                if (lastPostDate == null) {
                    userRepository.updateStreak(userId, 1L, today);
                } else if (lastPostDate.equals(today)) {
                } else if (lastPostDate.equals(today.minusDays(1))) {
                    userRepository.updateStreak(userId, currentStreak + 1, today);
                } else {
                    userRepository.updateStreak(userId, 1L, today);
                }
            }

            long existingPostsInBeacon = postRepository.countByUserIdAndBeaconId(userId, beaconId);
            if (existingPostsInBeacon == 1) {
                userRepository.increaseVisitedPlaceCount(userId);
            }

        } catch (Exception e) {
            log.error("유저 통계 업데이트 실패: {}", e.getMessage());
            throw e;
        }
    }
}