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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
        List<Long> recentPosters = postRepository.findUserIdsWhoPostedBetween(now.minusHours(24), now.plusHours(24));
        Set<Long> recentPosterSet = Set.copyOf(recentPosters);

        int pageNumber = 0;
        boolean hasNext = true;
        int totalProcessed = 0;

        while (hasNext) {
            Pageable pageable = PageRequest.of(pageNumber, BATCH_SIZE);
            Slice<User> userSlice = userRepository.findActiveUsersWithFcmToken(pageable);
            List<User> candidates = userSlice.getContent();

            List<User> finalTargets = new ArrayList<>();
            List<DailyPushLog> logsToSave = new ArrayList<>();

            for (User user : candidates) {
                ZoneId userZone;
                try {
                    userZone = ZoneId.of(user.getTimezone() != null ? user.getTimezone() : "Asia/Seoul");
                } catch (Exception e) {
                    userZone = ZoneId.of("Asia/Seoul");
                }
                LocalDate localToday = LocalDate.now(userZone);
                String logId = localToday.toString() + "_" + user.getId();

                if (dailyPushLogRepository.existsById(logId)) {
                    continue;
                }

                if (recentPosterSet.contains(user.getId())) {
                    boolean postedToday = postRepository.findUserIdsWhoPostedBetween(
                            localToday.atStartOfDay(userZone).toLocalDateTime(),
                            localToday.plusDays(1).atStartOfDay(userZone).toLocalDateTime()
                    ).contains(user.getId());
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