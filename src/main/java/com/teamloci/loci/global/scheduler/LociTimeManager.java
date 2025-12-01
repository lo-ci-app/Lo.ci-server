package com.teamloci.loci.global.scheduler;

import com.teamloci.loci.domain.DailyPushLog;
import com.teamloci.loci.domain.User;
import com.teamloci.loci.repository.DailyPushLogRepository;
import com.teamloci.loci.repository.UserRepository;
import com.teamloci.loci.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class LociTimeManager {

    private final TaskScheduler taskScheduler;
    private final UserRepository userRepository;
    private final DailyPushLogRepository dailyPushLogRepository;
    private final NotificationService notificationService;

    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");

    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void scheduleDailyLoci() {
        dailyPushLogRepository.truncateTable();

        long randomSeconds = ThreadLocalRandom.current().nextLong(9 * 3600, 22 * 3600);
        LocalDateTime todayLociTime = LocalDateTime.of(LocalDate.now(SEOUL_ZONE), LocalTime.ofSecondOfDay(randomSeconds));

        log.info("📅 오늘의 Loci Time: {}", todayLociTime);

        taskScheduler.schedule(this::executeGlobalPush, todayLociTime.atZone(SEOUL_ZONE).toInstant());
    }

    @Transactional
    public void executeGlobalPush() {
        log.info("🔔 [Global Push] 로키 타임 알림 발송 시작!");
        LocalDate today = LocalDate.now(SEOUL_ZONE);

        List<Long> excludedUserIds = dailyPushLogRepository.findAllUserIds();

        List<User> targetUsers;
        if (excludedUserIds.isEmpty()) {
            targetUsers = userRepository.findActiveUsersWithFcmToken();
        } else {
            targetUsers = userRepository.findActiveUsersWithFcmTokenExcludingIds(excludedUserIds);
        }

        if (!targetUsers.isEmpty()) {
            notificationService.sendMulticast(
                    targetUsers,
                    com.teamloci.loci.domain.NotificationType.NEW_POST,
                    "Time to Loci! 📸",
                    "지금 바로 친구들에게 일상을 공유하세요!",
                    null
            );

            List<DailyPushLog> logs = targetUsers.stream()
                    .map(u -> DailyPushLog.builder()
                            .userId(u.getId())
                            .date(today)
                            .build())
                    .collect(Collectors.toList());
            dailyPushLogRepository.saveAll(logs);
        }

        log.info("🔔 [Global Push] 발송 완료: 총 {}명 (이미 받은 {}명 제외)", targetUsers.size(), excludedUserIds.size());
    }
}