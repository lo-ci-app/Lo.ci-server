package com.teamloci.loci.global.util;

import com.teamloci.loci.domain.notification.DailyPushLog;
import com.teamloci.loci.domain.notification.DailyPushLogRepository;
import com.teamloci.loci.domain.notification.NotificationService;
import com.teamloci.loci.domain.notification.NotificationType;
import com.teamloci.loci.domain.post.repository.PostRepository; // [추가]
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
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LociPushService {

    private final UserRepository userRepository;
    private final DailyPushLogRepository dailyPushLogRepository;
    private final NotificationService notificationService;
    private final PostRepository postRepository;

    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");
    private static final int BATCH_SIZE = 1000;

    @Transactional
    public void executeGlobalPush() {
        log.info("🔔 [Global Push] 로키 타임 알림 발송 시작!");
        LocalDate today = LocalDate.now(SEOUL_ZONE);

        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.plusDays(1).atStartOfDay();

        List<Long> excludedUserIds = new ArrayList<>(dailyPushLogRepository.findAllUserIds());

        List<Long> postedUserIds = postRepository.findUserIdsWhoPostedBetween(startOfDay, endOfDay);
        excludedUserIds.addAll(postedUserIds);

        excludedUserIds = excludedUserIds.stream().distinct().toList();

        int pageNumber = 0;
        boolean hasNext = true;
        int totalProcessed = 0;

        while (hasNext) {
            Pageable pageable = PageRequest.of(pageNumber, BATCH_SIZE);
            Slice<User> userSlice;

            if (excludedUserIds.isEmpty()) {
                userSlice = userRepository.findActiveUsersWithFcmToken(pageable);
            } else {
                userSlice = userRepository.findActiveUsersWithFcmTokenExcludingIds(excludedUserIds, pageable);
            }

            List<User> targetUsers = userSlice.getContent();

            if (!targetUsers.isEmpty()) {
                notificationService.sendMulticast(
                        targetUsers,
                        NotificationType.LOCI_TIME,
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

                totalProcessed += targetUsers.size();
            }

            hasNext = userSlice.hasNext();
            pageNumber++;
        }

        log.info("🔔 [Global Push] 발송 완료: 총 {}명 (이미 받거나 작성한 {}명 제외)", totalProcessed, excludedUserIds.size());
    }
}