package com.teamloci.loci.global.util;

import com.teamloci.loci.domain.notification.DailyPushLog;
import com.teamloci.loci.domain.notification.DailyPushLogRepository;
import com.teamloci.loci.domain.notification.NotificationMessageProvider;
import com.teamloci.loci.domain.notification.NotificationService;
import com.teamloci.loci.domain.notification.NotificationType;
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
    private final NotificationMessageProvider messageProvider;

    private static final int BATCH_SIZE = 1000;

    @Transactional
    public void executeGlobalPush() {
        log.info("🔔 [Global Push] 로키 타임 알림 발송 시작!");

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

                finalTargets.add(user);
                logsToSave.add(new DailyPushLog(logId, user.getId(), localToday));
            }

            if (!finalTargets.isEmpty()) {
                Map<String, List<User>> usersByCountry = finalTargets.stream()
                        .collect(Collectors.groupingBy(
                                u -> u.getCountryCode() != null ? u.getCountryCode() : NotificationMessageProvider.DEFAULT_LANG
                        ));

                usersByCountry.forEach((countryCode, users) -> {
                    var content = messageProvider.getMessage(NotificationType.LOCI_TIME, countryCode);
                    List<Long> targetIds = users.stream().map(User::getId).toList();

                    notificationService.sendMulticast(
                            targetIds,
                            NotificationType.LOCI_TIME,
                            content.title(),
                            content.body(),
                            null,
                            null
                    );
                });

                dailyPushLogRepository.saveAll(logsToSave);
                totalProcessed += finalTargets.size();
            }

            hasNext = userSlice.hasNext();
            pageNumber++;
        }

        log.info("🔔 [Global Push] 발송 완료: 총 {}명", totalProcessed);
    }
}