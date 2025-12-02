package com.teamloci.loci.global.util;

import com.teamloci.loci.domain.notification.DailyPushLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Component
@RequiredArgsConstructor
public class LociTimeManager {

    private final TaskScheduler taskScheduler;
    private final DailyPushLogRepository dailyPushLogRepository;
    private final LociPushService lociPushService;

    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");

    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    @Transactional
    public void scheduleDailyLoci() {
        dailyPushLogRepository.truncateTable();
        schedulePushForDate(LocalDate.now(SEOUL_ZONE));
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        LocalDate today = LocalDate.now(SEOUL_ZONE);

        boolean alreadySent = dailyPushLogRepository.existsByDate(today);
        if (alreadySent) {
            log.info("✅ [Recovery] 오늘의 Loci 알림은 이미 발송되었습니다. 스케줄링을 건너뜁니다.");
            return;
        }

        log.info("🔄 [Recovery] 서버 재시작 감지: 오늘의 알림 스케줄을 재설정합니다.");
        schedulePushForDate(today);
    }

    private void schedulePushForDate(LocalDate date) {
        LocalDateTime now = LocalDateTime.now(SEOUL_ZONE);

        LocalDateTime startRange = date.atTime(10, 0);
        LocalDateTime endRange = date.atTime(20, 0);

        if (now.isAfter(endRange)) {
            log.info("🕒 [Schedule] 오늘의 발송 가능 시간(20:00)이 지났습니다.");
            return;
        }

        LocalDateTime scheduleTime;

        if (now.isAfter(startRange)) {
            long secondsLeft = Duration.between(now, endRange).getSeconds();
            long randomSeconds = ThreadLocalRandom.current().nextLong(0, secondsLeft);
            scheduleTime = now.plusSeconds(randomSeconds);
        } else {
            long startSeconds = 10 * 3600;
            long endSeconds = 20 * 3600;
            long randomSeconds = ThreadLocalRandom.current().nextLong(startSeconds, endSeconds);
            scheduleTime = date.atStartOfDay().plusSeconds(randomSeconds);
        }

        log.info("📅 [Schedule] Loci Time 예약 완료: {}", scheduleTime);
        taskScheduler.schedule(() -> lociPushService.executeGlobalPush(), scheduleTime.atZone(SEOUL_ZONE).toInstant());
    }
}