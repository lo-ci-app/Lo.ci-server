package com.teamloci.loci.domain.post.service;

import com.teamloci.loci.domain.friend.Friendship;
import com.teamloci.loci.domain.friend.FriendshipRepository;
import com.teamloci.loci.domain.notification.NotificationService;
import com.teamloci.loci.domain.notification.NotificationType;
import com.teamloci.loci.domain.post.dto.ReactionDto;
import com.teamloci.loci.domain.post.entity.*;
import com.teamloci.loci.domain.post.repository.CommentLikeRepository;
import com.teamloci.loci.domain.post.repository.PostCommentRepository;
import com.teamloci.loci.domain.post.repository.PostReactionRepository;
import com.teamloci.loci.domain.post.repository.PostRepository;
import com.teamloci.loci.domain.user.User;
import com.teamloci.loci.domain.user.UserDto;
import com.teamloci.loci.domain.user.UserRepository;
import com.teamloci.loci.domain.user.service.UserActivityService;
import com.teamloci.loci.global.error.CustomException;
import com.teamloci.loci.global.error.ErrorCode;
import com.teamloci.loci.global.util.RelationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ReactionService {

    private final PostReactionRepository postReactionRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final PostRepository postRepository;
    private final PostCommentRepository commentRepository;
    private final UserRepository userRepository;
    private final FriendshipRepository friendshipRepository;
    private final NotificationService notificationService;
    private final UserActivityService userActivityService;

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    @Transactional
    public void togglePostReaction(Long userId, Long postId, ReactionType type) {
        User user = findUser(userId);
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        Optional<PostReaction> existing = postReactionRepository.findByPostIdAndUserId(postId, userId);

        if (existing.isPresent()) {
            PostReaction reaction = existing.get();
            if (reaction.getType() == type) {
                postReactionRepository.delete(reaction);
            } else {
                reaction.changeType(type);
            }
        } else {
            postReactionRepository.save(PostReaction.builder()
                    .post(post)
                    .user(user)
                    .type(type)
                    .build());

            if (!post.getUser().getId().equals(userId)) {
                notificationService.send(
                        post.getUser(),
                        NotificationType.POST_REACTION,
                        "새로운 반응",
                        user.getNickname() + "님이 회원님의 게시물에 반응을 남겼습니다.",
                        postId
                );
            }
        }
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

        // [Bulk 조회 적용]
        Map<Long, UserActivityService.UserStats> statsMap = userActivityService.getUserStatsMap(new ArrayList<>(userIds));

        List<ReactionDto.Response> dtos = resultList.stream()
                .map(r -> {
                    User u = r.getUser();
                    String status;

                    if (u.getId().equals(myUserId)) {
                        status = "SELF";
                    } else {
                        status = RelationUtil.resolveStatus(friendshipMap.get(u.getId()), myUserId);
                    }

                    var stats = statsMap.getOrDefault(u.getId(), new UserActivityService.UserStats(0,0,0,0));

                    UserDto.UserResponse userResponse = UserDto.UserResponse.of(
                            u, status, stats.friendCount(), stats.postCount(), stats.streakCount(), stats.visitedPlaceCount()
                    );

                    return ReactionDto.Response.of(r, userResponse);
                })
                .collect(Collectors.toList());

        return ReactionDto.ListResponse.builder()
                .reactions(dtos)
                .hasNext(hasNext)
                .nextCursor(nextCursor)
                .build();
    }

    public void toggleCommentLike(Long userId, Long commentId) {
        User user = findUser(userId);
        PostComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND));

        Optional<CommentLike> existing = commentLikeRepository.findByCommentIdAndUserId(commentId, userId);

        if (existing.isPresent()) {
            commentLikeRepository.delete(existing.get());
        } else {
            commentLikeRepository.save(CommentLike.builder()
                    .comment(comment)
                    .user(user)
                    .build());

            if (!comment.getUser().getId().equals(userId)) {
                notificationService.send(
                        comment.getUser(),
                        NotificationType.COMMENT_LIKE,
                        "댓글 좋아요",
                        user.getNickname() + "님이 회원님의 댓글을 좋아합니다.",
                        comment.getPost().getId()
                );
            }
        }
    }
}