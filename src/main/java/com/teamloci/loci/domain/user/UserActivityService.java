package com.teamloci.loci.domain.user.service;

import com.teamloci.loci.domain.friend.FriendshipRepository;
import com.teamloci.loci.domain.post.entity.PostStatus;
import com.teamloci.loci.domain.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserActivityService {

    private final PostRepository postRepository;
    private final FriendshipRepository friendshipRepository;

    public record UserStats(long friendCount, long postCount, long streakCount, long visitedPlaceCount) {}

    public UserStats getUserStats(Long userId) {
        return getUserStatsMap(List.of(userId)).getOrDefault(userId, new UserStats(0, 0, 0, 0));
    }

    public Map<Long, UserStats> getUserStatsMap(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Long> distinctIds = userIds.stream().distinct().toList();

        Map<Long, Long> friendCounts = new HashMap<>();
        friendshipRepository.countFriendsByUserIds(distinctIds).forEach(row ->
                friendCounts.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue())
        );

        Map<Long, Long> postCounts = new HashMap<>();
        postRepository.countPostsByUserIds(distinctIds, PostStatus.ACTIVE).forEach(row ->
                postCounts.put((Long) row[0], (Long) row[1])
        );

        Map<Long, Long> visitedPlaceCounts = new HashMap<>();
        postRepository.countDistinctBeaconsByUserIds(distinctIds).forEach(row ->
                visitedPlaceCounts.put((Long) row[0], (Long) row[1])
        );

        Map<Long, Long> streakCounts = calculateStreakBulk(distinctIds);

        Map<Long, UserStats> result = new HashMap<>();
        for (Long userId : distinctIds) {
            result.put(userId, new UserStats(
                    friendCounts.getOrDefault(userId, 0L),
                    postCounts.getOrDefault(userId, 0L),
                    streakCounts.getOrDefault(userId, 0L),
                    visitedPlaceCounts.getOrDefault(userId, 0L)
            ));
        }
        return result;
    }

    private Map<Long, Long> calculateStreakBulk(List<Long> userIds) {
        List<Object[]> rows = postRepository.findPostDatesByUserIds(userIds);

        Map<Long, List<LocalDate>> userPostDates = new HashMap<>();
        for (Object[] row : rows) {
            Long userId = ((Number) row[0]).longValue();
            java.sql.Date sqlDate = (java.sql.Date) row[1];
            userPostDates.computeIfAbsent(userId, k -> new ArrayList<>()).add(sqlDate.toLocalDate());
        }

        Map<Long, Long> streaks = new HashMap<>();
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        for (Long userId : userIds) {
            List<LocalDate> dates = userPostDates.getOrDefault(userId, Collections.emptyList());

            if (dates.isEmpty()) {
                streaks.put(userId, 0L);
                continue;
            }

            List<LocalDate> uniqueDates = dates.stream().distinct().toList();

            LocalDate lastPostDate = uniqueDates.get(0);
            if (!lastPostDate.equals(today) && !lastPostDate.equals(yesterday)) {
                streaks.put(userId, 0L);
                continue;
            }

            long streak = 0;
            LocalDate checkDate = lastPostDate.equals(today) ? today : yesterday;

            for (LocalDate date : uniqueDates) {
                if (date.equals(checkDate)) {
                    streak++;
                    checkDate = checkDate.minusDays(1);
                } else {
                    break;
                }
            }
            streaks.put(userId, streak);
        }
        return streaks;
    }
}