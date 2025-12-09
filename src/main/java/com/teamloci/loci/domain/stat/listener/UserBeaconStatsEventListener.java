package com.teamloci.loci.domain.stat.listener;

import com.teamloci.loci.domain.post.entity.Post;
import com.teamloci.loci.domain.post.event.PostCreatedEvent;
import com.teamloci.loci.domain.stat.entity.UserBeaconStats;
import com.teamloci.loci.domain.stat.repository.UserBeaconStatsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserBeaconStatsEventListener {

    private final UserBeaconStatsRepository statsRepository;

    @Async
    @EventListener
    @Transactional
    public void handlePostCreated(PostCreatedEvent event) {
        Post post = event.getPost();

        try {
            UserBeaconStats stats = statsRepository.findByUserIdAndBeaconId(post.getUser().getId(), post.getBeaconId())
                    .orElseGet(() -> UserBeaconStats.builder()
                            .userId(post.getUser().getId())
                            .beaconId(post.getBeaconId())
                            .latitude(post.getLatitude())
                            .longitude(post.getLongitude())
                            .postCount(0L)
                            .latestThumbnailUrl(post.getThumbnailUrl())
                            .build());

            stats.incrementCount(post.getThumbnailUrl());
            statsRepository.save(stats);

        } catch (Exception e) {
            log.error("[Stats Error] 통계 데이터 갱신 실패 post_id={}", post.getId(), e);
        }
    }
}