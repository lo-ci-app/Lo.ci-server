package com.teamloci.loci.domain.post.service;

import com.teamloci.loci.domain.friend.Friendship;
import com.teamloci.loci.domain.friend.FriendshipRepository;
import com.teamloci.loci.domain.intimacy.entity.FriendshipIntimacy;
import com.teamloci.loci.domain.intimacy.entity.IntimacyType;
import com.teamloci.loci.domain.intimacy.service.IntimacyService;
import com.teamloci.loci.domain.notification.NotificationService;
import com.teamloci.loci.domain.notification.NotificationType;
import com.teamloci.loci.domain.post.dto.ReactionDto;
import com.teamloci.loci.domain.post.entity.*;
import com.teamloci.loci.domain.post.repository.PostCommentRepository;
import com.teamloci.loci.domain.post.repository.PostReactionRepository;
import com.teamloci.loci.domain.post.repository.PostRepository;
import com.teamloci.loci.domain.user.*;
import com.teamloci.loci.global.error.CustomException;
import com.teamloci.loci.global.error.ErrorCode;
import com.teamloci.loci.global.util.RelationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ReactionService {

    private final PostReactionRepository postReactionRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final FriendshipRepository friendshipRepository;
    private final NotificationService notificationService;
    private final UserActivityService userActivityService;
    private final IntimacyService intimacyService;
    private final StringRedisTemplate redisTemplate;

    private static final String REACTION_NOTI_COOLTIME_PREFIX = "noti:cooltime:reaction:";
    private static final long NOTI_COOLTIME_SECONDS = 30;

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    @Transactional
    public void togglePostReaction(Long userId, Long postId, ReactionType type) {
        User user = findUser(userId);

        Post post = postRepository.findByIdWithDetails(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        Optional<PostReaction> existing = postReactionRepository.findByPostIdAndUserId(postId, userId);

        if (existing.isPresent()) {
            PostReaction reaction = existing.get();
            if (reaction.getType() == type) {
                postReactionRepository.delete(reaction);
                postRepository.decreaseReactionCount(postId);
            } else {
                reaction.changeType(type);
            }
        } else {
            try {
                postReactionRepository.saveAndFlush(PostReaction.builder()
                        .post(post)
                        .user(user)
                        .type(type)
                        .build());

                postRepository.increaseReactionCount(postId);

                if (!post.getUser().getId().equals(userId)) {
                    intimacyService.accumulatePoint(userId, post.getUser().getId(), IntimacyType.REACTION, null);

                    sendReactionNotification(user, post, postId);
                }

            } catch (DataIntegrityViolationException e) {
                log.warn("이미 반응이 존재합니다 (Race Condition Ignored): userId={}, postId={}", userId, postId);
                throw new CustomException(ErrorCode.TOO_MANY_REQUESTS);
            }
        }
    }

    private void sendReactionNotification(User sender, Post post, Long postId) {
        String redisKey = REACTION_NOTI_COOLTIME_PREFIX + postId + ":" + sender.getId();

        if (Boolean.TRUE.equals(redisTemplate.hasKey(redisKey))) {
            return;
        }

        Set<User> recipients = new HashSet<>();

        if (post.getUser().getStatus() == UserStatus.ACTIVE) {
            recipients.add(post.getUser());
        }

        if (post.getCollaborators() != null) {
            post.getCollaborators().stream()
                    .map(PostCollaborator::getUser)
                    .filter(u -> u.getStatus() == UserStatus.ACTIVE)
                    .forEach(recipients::add);
        }

        for (User recipient : recipients) {
            if (recipient.getId().equals(sender.getId())) continue;

            notificationService.send(
                    recipient,
                    NotificationType.POST_REACTION,
                    postId,
                    post.getThumbnailUrl(),
                    sender.getNickname()
            );
        }

        redisTemplate.opsForValue().set(redisKey, "1", Duration.ofSeconds(NOTI_COOLTIME_SECONDS));
    }

    public ReactionDto.ListResponse getReactions(Long myUserId, Long postId, Long cursorId, int size) {
        if (!postRepository.existsById(postId)) {
            throw new CustomException(ErrorCode.POST_NOT_FOUND);
        }

        List<PostReaction> resultList = new ArrayList<>();

        if (cursorId == null) {
            postReactionRepository.findByPostIdAndUserIdWithUser(postId, myUserId)
                    .ifPresent(resultList::add);
        }

        int limit = (cursorId == null && !resultList.isEmpty()) ? size - 1 : size;

        PageRequest pageable = PageRequest.of(0, limit + 1);
        List<PostReaction> others = postReactionRepository.findByPostIdAndUserIdNotWithCursor(postId, myUserId, cursorId, pageable);

        boolean hasNext = false;
        if (others.size() > limit) {
            hasNext = true;
            others.remove(limit);
        }

        resultList.addAll(others);

        Long nextCursor = others.isEmpty() ? null : others.get(others.size() - 1).getId();

        Set<Long> userIds = resultList.stream()
                .map(r -> r.getUser().getId())
                .collect(Collectors.toSet());

        Set<Long> otherIds = userIds.stream().filter(id -> !id.equals(myUserId)).collect(Collectors.toSet());
        Map<Long, Friendship> friendshipMap;

        if (otherIds.isEmpty()) {
            friendshipMap = Map.of();
        } else {
            friendshipMap = friendshipRepository.findAllRelationsBetween(myUserId, new ArrayList<>(otherIds)).stream()
                    .collect(Collectors.toMap(
                            f -> f.getRequester().getId().equals(myUserId) ? f.getReceiver().getId() : f.getRequester().getId(),
                            f -> f
                    ));
        }

        Map<Long, UserActivityService.UserStats> statsMap = userActivityService.getUserStatsMap(new ArrayList<>(userIds));
        Map<Long, FriendshipIntimacy> intimacyMap = intimacyService.getIntimacyMap(myUserId);

        List<ReactionDto.Response> dtos = resultList.stream()
                .map(r -> {
                    User u = r.getUser();
                    String status;

                    if (u.getId().equals(myUserId)) {
                        status = "SELF";
                    } else {
                        status = RelationUtil.resolveStatus(friendshipMap.get(u.getId()), myUserId);
                    }

                    var stats = statsMap.getOrDefault(u.getId(), new UserActivityService.UserStats(0,0,0,0,0));

                    UserDto.UserResponse userResponse = UserDto.UserResponse.of(
                            u, status, stats.friendCount(), stats.postCount(), stats.streakCount(), stats.visitedPlaceCount()
                    );

                    userResponse.applyIntimacyInfo(intimacyMap.get(u.getId()), stats.totalIntimacyLevel());

                    return ReactionDto.Response.of(r, userResponse);
                })
                .collect(Collectors.toList());

        return ReactionDto.ListResponse.builder()
                .reactions(dtos)
                .hasNext(hasNext)
                .nextCursor(nextCursor)
                .build();
    }
}