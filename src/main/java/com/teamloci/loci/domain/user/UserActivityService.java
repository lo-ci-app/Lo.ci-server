package com.teamloci.loci.domain.user;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserActivityService {

    private final UserRepository userRepository;

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
}