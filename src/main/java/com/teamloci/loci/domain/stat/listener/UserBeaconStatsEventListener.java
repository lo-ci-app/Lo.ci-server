package com.teamloci.loci.domain.stat.listener;

import com.teamloci.loci.domain.post.entity.Post;
import com.teamloci.loci.domain.post.event.PostCreatedEvent;
import com.teamloci.loci.domain.stat.entity.UserBeaconStats;
import com.teamloci.loci.domain.stat.repository.UserBeaconStatsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserBeaconStatsEventListener {

    private final UserBeaconStatsRepository statsRepository;
    private final PlatformTransactionManager transactionManager;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePostCreated(PostCreatedEvent event) {
        Post post = event.getPost();

        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        int maxRetries = 3;
        for (int i = 0; i < maxRetries; i++) {
            try {
                template.execute(status -> {
                    updateStats(post);
                    return null;
                });
                return;
            } catch (DataIntegrityViolationException e) {
                log.warn("[Stats Retry] 동시성 충돌 발생, 재시도합니다 ({}회). userId={}, beaconId={}",
                        i + 1, post.getUser().getId(), post.getBeaconId());
                try {
                    Thread.sleep(50);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            } catch (Exception e) {
                log.error("[Stats Error] 통계 갱신 실패", e);
                return;
            }
        }
    }

    private void updateStats(Post post) {
        UserBeaconStats stats = statsRepository.findByUserIdAndBeaconId(post.getUser().getId(), post.getBeaconId())
                .orElseGet(() -> UserBeaconStats.builder()
                        .userId(post.getUser().getId())
                        .beaconId(post.getBeaconId())
                        .latitude(post.getLatitude())
                        .longitude(post.getLongitude())
                        .postCount(0L)
                        .latestThumbnailUrl(null)
                        .latestPostedAt(null)
                        .build());

        stats.updateStats(post.getThumbnailUrl(), post.getCreatedAt());
        statsRepository.save(stats);
    }
}