package com.teamloci.loci.domain.badge;

import com.teamloci.loci.domain.auth.event.UserLoginEvent;
import com.teamloci.loci.domain.intimacy.entity.IntimacyType;
import com.teamloci.loci.domain.intimacy.event.IntimacyLevelUpEvent;
import com.teamloci.loci.domain.intimacy.event.NudgeEvent;
import com.teamloci.loci.domain.intimacy.repository.IntimacyLogRepository;
import com.teamloci.loci.domain.post.event.CommentCreatedEvent;
import com.teamloci.loci.domain.post.event.PostCreatedEvent;
import com.teamloci.loci.domain.post.repository.PostCommentRepository;
import com.teamloci.loci.domain.post.repository.PostRepository;
import com.teamloci.loci.domain.stat.repository.UserBeaconStatsRepository;
import com.teamloci.loci.domain.user.User;
import com.teamloci.loci.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
@RequiredArgsConstructor
public class BadgeEventListener {

    private final BadgeService badgeService;
    private final PostRepository postRepository;
    private final PostCommentRepository postCommentRepository;
    private final UserBeaconStatsRepository userBeaconStatsRepository;
    private final IntimacyLogRepository intimacyLogRepository;
    private final UserRepository userRepository;

    @Async
    @Order(2)
    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handlePostCreated(PostCreatedEvent event) {
        User user = event.getPost().getUser();

        if (user.getStreakCount() >= 30) {
            badgeService.awardBadge(user, BadgeType.PERFECT_ATTENDANCE);
        }

        ZoneId userZoneId = user.getZoneIdOrDefault();
        LocalTime now = LocalTime.now(userZoneId);

        String timeOffset = ZonedDateTime.now(userZoneId)
                .format(DateTimeFormatter.ofPattern("xxx"));

        if (now.isAfter(LocalTime.of(6, 0)) && now.isBefore(LocalTime.of(8, 0))) {
            long count = postRepository.countByUserAndCreatedHourBetween(user.getId(), 6, 8, timeOffset);
            if (count >= 10) badgeService.awardBadge(user, BadgeType.EARLY_BIRD);
        }

        if (now.isAfter(LocalTime.of(22, 0)) || now.isBefore(LocalTime.of(0, 0))) {
            long count = postRepository.countByUserAndCreatedHourBetween(user.getId(), 22, 24, timeOffset);
            if (count >= 10) badgeService.awardBadge(user, BadgeType.OWL);
        }

        if (user.getVisitedPlaceCount() >= 20) {
            badgeService.awardBadge(user, BadgeType.EXPLORER);
        }

        if (event.getPost().getCollaborators() != null && !event.getPost().getCollaborators().isEmpty()) {
            badgeService.awardBadge(user, BadgeType.FIRST_ENCOUNTER);
        }

        String beaconId = event.getPost().getBeaconId();
        if (beaconId != null) {
            userBeaconStatsRepository.findByUserIdAndBeaconId(user.getId(), beaconId).ifPresent(stats -> {
                if (stats.getPostCount() >= 30) {
                    badgeService.awardBadge(user, BadgeType.THE_LANDLORD);
                }
            });
        }
    }

    @Async
    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleCommentCreated(CommentCreatedEvent event) {
        User user = event.getUser();
        long count = postCommentRepository.countByUser(user);
        if (count >= 100) {
            badgeService.awardBadge(user, BadgeType.HEAVY_TALKER);
        }
    }

    @Async
    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleIntimacyLevelUp(IntimacyLevelUpEvent event) {
        if (event.getNewLevel() >= 7) {
            userRepository.findById(event.getActorId())
                    .ifPresent(u -> badgeService.awardBadge(u, BadgeType.SOULMATE));
            userRepository.findById(event.getTargetId())
                    .ifPresent(u -> badgeService.awardBadge(u, BadgeType.SOULMATE));
        }
    }

    @Async
    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleNudge(NudgeEvent event) {
        User receiver = event.getReceiver();
        long count = intimacyLogRepository.countByTargetIdAndType(receiver.getId(), IntimacyType.NUDGE);
        if (count >= 50) {
            badgeService.awardBadge(receiver, BadgeType.BELOVED);
        }
    }

    @Async
    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleUserLogin(UserLoginEvent event) {
        badgeService.awardBadge(event.getUser(), BadgeType.NEWBIE);
    }
}