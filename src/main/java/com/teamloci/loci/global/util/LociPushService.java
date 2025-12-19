package com.teamloci.loci.global.util;

import com.teamloci.loci.domain.notification.DailyPushLog;
import com.teamloci.loci.domain.notification.DailyPushLogRepository;
import com.teamloci.loci.domain.notification.NotificationService;
import com.teamloci.loci.domain.notification.NotificationType;
import com.teamloci.loci.domain.post.repository.PostRepository;
import com.teamloci.loci.domain.user.User;
import com.teamloci.loci.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LociPushService {

    private final UserRepository userRepository;
    private final DailyPushLogRepository dailyPushLogRepository;
    private final NotificationService notificationService;
    private final PostRepository postRepository;

    private static final int BATCH_SIZE = 1000;

    @Transactional
    public void executeGlobalPush() {
        log.info("🔔 [Global Push] 로키 타임 알림 발송 시작!");

        LocalDateTime now = LocalDateTime.now();

        List<Object[]> recentPosts = postRepository.findPostTimestampsBetween(now.minusHours(24), now.plusHours(24));

        Map<Long, List<LocalDateTime>> recentPostMap = new HashMap<>();
        for (Object[] row : recentPosts) {
            Long uid = (Long) row[0];
            LocalDateTime createdAt = (LocalDateTime) row[1];
            recentPostMap.computeIfAbsent(uid, k -> new ArrayList<>()).add(createdAt);
        }

        int pageNumber = 0;
        boolean hasNext = true;
        int totalProcessed = 0;

        while (hasNext) {
            Pageable pageable = PageRequest.of(pageNumber, BATCH_SIZE);
            Slice<User> userSlice = userRepository.findActiveUsersWithFcmToken(pageable);
            List<User> candidates = userSlice.getContent();

            List<String> candidateLogIds = new ArrayList<>();
            for (User user : candidates) {
                ZoneId userZone = user.getZoneIdOrDefault();
                LocalDate localToday = LocalDate.now(userZone);
                candidateLogIds.add(localToday.toString() + "_" + user.getId());
            }

            Set<String> existingLogIds = dailyPushLogRepository.findAllById(candidateLogIds).stream()
                    .map(DailyPushLog::getId)
                    .collect(Collectors.toSet());

            List<User> finalTargets = new ArrayList<>();
            List<DailyPushLog> logsToSave = new ArrayList<>();

            for (User user : candidates) {
                if (!user.isLociTimePushEnabled()) {
                    continue;
                }

                ZoneId userZone = user.getZoneIdOrDefault();
                LocalDate localToday = LocalDate.now(userZone);
                String logId = localToday.toString() + "_" + user.getId();

                if (existingLogIds.contains(logId)) {
                    continue;
                }

                if (recentPostMap.containsKey(user.getId())) {
                    LocalDateTime startOfToday = localToday.atStartOfDay(userZone).toLocalDateTime();
                    LocalDateTime endOfToday = localToday.plusDays(1).atStartOfDay(userZone).toLocalDateTime();

                    boolean postedToday = recentPostMap.get(user.getId()).stream()
                            .anyMatch(date -> !date.isBefore(startOfToday) && date.isBefore(endOfToday));

                    if (postedToday) continue;
                }

                finalTargets.add(user);
                logsToSave.add(new DailyPushLog(logId, user.getId(), localToday));
            }

            if (!finalTargets.isEmpty()) {
                List<Long> targetIds = finalTargets.stream().map(User::getId).toList();

                notificationService.sendMulticast(
                        targetIds,
                        NotificationType.LOCI_TIME,
                        "Time to Loci! 📸",
                        "지금 바로 친구들에게 일상을 공유하세요!",
                        null,
                        null
                );

                dailyPushLogRepository.saveAll(logsToSave);
                totalProcessed += finalTargets.size();
            }

            hasNext = userSlice.hasNext();
            pageNumber++;
        }

        log.info("🔔 [Global Push] 발송 완료: 총 {}명", totalProcessed);
    }
}