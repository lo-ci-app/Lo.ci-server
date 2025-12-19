package com.teamloci.loci.domain.post.listener;

import com.teamloci.loci.domain.friend.FriendshipRepository;
import com.teamloci.loci.domain.intimacy.entity.IntimacyType;
import com.teamloci.loci.domain.intimacy.service.IntimacyService;
import com.teamloci.loci.domain.notification.DailyPushLog;
import com.teamloci.loci.domain.notification.DailyPushLogRepository;
import com.teamloci.loci.domain.notification.NotificationService;
import com.teamloci.loci.domain.notification.NotificationType;
import com.teamloci.loci.domain.post.entity.Post;
import com.teamloci.loci.domain.post.entity.PostCollaborator;
import com.teamloci.loci.domain.post.event.PostCreatedEvent;
import com.teamloci.loci.domain.post.repository.PostRepository;
import com.teamloci.loci.domain.user.User;
import com.teamloci.loci.domain.user.UserActivityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class PostEventListener {

    private final IntimacyService intimacyService;
    private final NotificationService notificationService;
    private final FriendshipRepository friendshipRepository;
    private final PostRepository postRepository;
    private final DailyPushLogRepository dailyPushLogRepository;
    private final CacheManager cacheManager;
    private final UserActivityService userActivityService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePostBusinessLogic(PostCreatedEvent event) {
        Post post = event.getPost();
        Long authorId = post.getUser().getId();

        userActivityService.updateUserStats(authorId, post.getBeaconId());

        if (post.getCollaborators() != null) {
            for (PostCollaborator collaborator : post.getCollaborators()) {
                Long collaboratorId = collaborator.getUser().getId();
                intimacyService.accumulatePoint(authorId, collaboratorId, IntimacyType.COLLABORATOR, null);

                if (!collaboratorId.equals(authorId)) {
                    notificationService.send(
                            collaborator.getUser(),
                            NotificationType.POST_TAGGED,
                            "함께한 순간",
                            post.getUser().getNickname() + "님이 회원님을 게시물에 태그했습니다.",
                            post.getId(),
                            post.getThumbnailUrl()
                    );
                }
            }
        }

        List<User> friends = friendshipRepository.findActiveFriendsByUserId(authorId);
        List<Long> friendIds = friends.stream().map(User::getId).toList();

        List<User> visitedFriends = postRepository.findUsersWhoPostedInBeacon(post.getBeaconId(), friendIds);

        for (User friend : visitedFriends) {
            intimacyService.accumulatePoint(authorId, friend.getId(), IntimacyType.VISIT, post.getBeaconId());
        }

        sendNotifications(post, friends, visitedFriends);

        evictUserStats(authorId);
    }

    private void evictUserStats(Long userId) {
        try {
            Cache cache = cacheManager.getCache("userStats");
            if (cache != null) {
                cache.evict(userId);
            }
        } catch (Exception e) {
            log.error("캐시 삭제 실패", e);
        }
    }

    private void sendNotifications(Post post, List<User> friends, List<User> visitedFriends) {
        try {
            User author = post.getUser();
            ZoneId authorZone = author.getZoneIdOrDefault();
            LocalDate today = LocalDate.now(authorZone);

            if (!friends.isEmpty()) {
                List<String> newPostLogIds = friends.stream()
                        .map(f -> today.toString() + "_" + f.getId())
                        .toList();

                Set<String> receivedLogIds = dailyPushLogRepository.findAllById(newPostLogIds).stream()
                        .map(DailyPushLog::getId)
                        .collect(Collectors.toSet());

                List<User> targetNewPostFriends = friends.stream()
                        .filter(User::isNewPostPushEnabled)
                        .filter(f -> !receivedLogIds.contains(today.toString() + "_" + f.getId()))
                        .collect(Collectors.toList());

                if (!targetNewPostFriends.isEmpty()) {
                    notificationService.sendMulticast(
                            targetNewPostFriends.stream().map(User::getId).toList(),
                            NotificationType.NEW_POST,
                            "새로운 Loci!",
                            author.getNickname() + "님이 지금 순간을 공유했어요 📸",
                            post.getId(),
                            post.getThumbnailUrl()
                    );

                    List<DailyPushLog> logs = targetNewPostFriends.stream()
                            .map(f -> DailyPushLog.builder()
                                    .userId(f.getId())
                                    .date(today)
                                    .build())
                            .collect(Collectors.toList());
                    dailyPushLogRepository.saveAll(logs);
                }
            }

            if (!visitedFriends.isEmpty()) {
                List<String> visitLogIds = visitedFriends.stream()
                        .map(f -> "VISIT_" + today.toString() + "_" + f.getId())
                        .toList();

                Set<String> alreadySentIds = dailyPushLogRepository.findAllById(visitLogIds).stream()
                        .map(DailyPushLog::getId)
                        .collect(Collectors.toSet());

                List<User> targetVisitedFriends = visitedFriends.stream()
                        .filter(f -> !alreadySentIds.contains("VISIT_" + today.toString() + "_" + f.getId()))
                        .collect(Collectors.toList());

                if (!targetVisitedFriends.isEmpty()) {
                    notificationService.sendMulticast(
                            targetVisitedFriends.stream().map(User::getId).toList(),
                            NotificationType.FRIEND_VISITED,
                            "반가운 발자취! 👣",
                            author.getNickname() + "님이 회원님이 방문했던 곳에 다녀갔어요!",
                            post.getId(),
                            post.getThumbnailUrl() 
                    );

                    List<DailyPushLog> logs = targetVisitedFriends.stream()
                            .map(f -> new DailyPushLog(
                                    "VISIT_" + today.toString() + "_" + f.getId(),
                                    f.getId(),
                                    today
                            ))
                            .collect(Collectors.toList());
                    dailyPushLogRepository.saveAll(logs);
                }
            }

        } catch (Exception e) {
            log.error("알림 발송 중 오류 발생: {}", e.getMessage());
        }
    }
}