package com.teamloci.loci.domain.post.listener;

import com.teamloci.loci.domain.friend.FriendshipRepository;
import com.teamloci.loci.domain.intimacy.entity.IntimacyType;
import com.teamloci.loci.domain.intimacy.service.IntimacyService;
import com.teamloci.loci.domain.notification.*;
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
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class PostEventListener {

    private final IntimacyService intimacyService;
    private final NotificationService notificationService;
    private final NotificationMessageProvider messageProvider;
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
                            post.getId(),
                            post.getThumbnailUrl(),
                            post.getUser().getNickname()
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

            Set<Long> taggedUserIds = (post.getCollaborators() != null) ?
                    post.getCollaborators().stream()
                            .map(c -> c.getUser().getId())
                            .collect(Collectors.toSet())
                    : Set.of();

            if (!friends.isEmpty()) {
                List<User> targetNewPostFriends = friends.stream()
                        .filter(User::isNewPostPushEnabled)
                        .filter(friend -> !taggedUserIds.contains(friend.getId()))
                        .toList();

                if (!targetNewPostFriends.isEmpty()) {
                    Map<String, List<User>> friendsByLang = targetNewPostFriends.stream()
                            .collect(Collectors.groupingBy(
                                    u -> u.getCountryCode() != null ? u.getCountryCode() : NotificationMessageProvider.DEFAULT_LANG
                            ));

                    friendsByLang.forEach((lang, group) -> {
                        var content = messageProvider.getMessage(NotificationType.NEW_POST, lang, author.getNickname());

                        notificationService.sendMulticast(
                                group.stream().map(User::getId).toList(),
                                NotificationType.NEW_POST,
                                content.title(),
                                content.body(),
                                post.getId(),
                                post.getThumbnailUrl()
                        );
                    });
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
                    Map<String, List<User>> visitedByLang = targetVisitedFriends.stream()
                            .collect(Collectors.groupingBy(
                                    u -> u.getCountryCode() != null ? u.getCountryCode() : NotificationMessageProvider.DEFAULT_LANG
                            ));

                    visitedByLang.forEach((lang, group) -> {
                        var content = messageProvider.getMessage(NotificationType.FRIEND_VISITED, lang, author.getNickname());

                        notificationService.sendMulticast(
                                group.stream().map(User::getId).toList(),
                                NotificationType.FRIEND_VISITED,
                                content.title(),
                                content.body(),
                                post.getId(),
                                post.getThumbnailUrl()
                        );
                    });

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