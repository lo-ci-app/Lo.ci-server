package com.teamloci.loci.global.util;

import com.teamloci.loci.domain.post.repository.PostRepository;
import com.teamloci.loci.domain.post.service.PostService; // PostService 추가
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class PostScheduler {

    private final PostRepository postRepository;
    private final PostService postService;

    private static final int EXPIRATION_DAYS = 30;

    @Scheduled(cron = "0 0 4 * * *")
    @Transactional
    public void archiveExpiredPosts() {
        LocalDateTime expiryDate = LocalDateTime.now().minusDays(EXPIRATION_DAYS);

        log.info("[Scheduler] 게시글 자동 보관 작업 시작... (기준: {} 이전 작성)", expiryDate);

        try {
            List<Object[]> targets = postRepository.findTargetsToArchive(expiryDate);
            Set<String> syncKeys = new HashSet<>();

            for (Object[] row : targets) {
                Long userId = (Long) row[0];
                String beaconId = (String) row[1];
                syncKeys.add(userId + ":" + beaconId);
            }

            int count = postRepository.archiveOldPosts(expiryDate);
            log.info("[Scheduler] 총 {}개의 게시글 상태가 ARCHIVED로 변경되었습니다.", count);

            if (count > 0 && !syncKeys.isEmpty()) {
                log.info("[Scheduler] {}개의 위치에 대해 통계 동기화를 수행합니다.", syncKeys.size());

                for (String key : syncKeys) {
                    String[] parts = key.split(":");
                    Long userId = Long.parseLong(parts[0]);
                    String beaconId = parts[1];

                    postService.recalculateBeaconStats(userId, beaconId);
                }
            }

            log.info("[Scheduler] 보관 및 통계 동기화 작업 완료.");

        } catch (Exception e) {
            log.error("[Scheduler] 게시글 보관 처리 중 오류 발생", e);
        }
    }
}