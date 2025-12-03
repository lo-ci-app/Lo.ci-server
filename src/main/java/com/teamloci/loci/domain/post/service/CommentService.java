package com.teamloci.loci.domain.post.service;

import com.teamloci.loci.domain.friend.Friendship;
import com.teamloci.loci.domain.friend.FriendshipStatus;
import com.teamloci.loci.domain.notification.NotificationType;
import com.teamloci.loci.domain.post.dto.CommentDto;
import com.teamloci.loci.domain.post.entity.Post;
import com.teamloci.loci.domain.post.entity.PostComment;
import com.teamloci.loci.domain.post.entity.PostStatus;
import com.teamloci.loci.domain.post.repository.PostCommentRepository;
import com.teamloci.loci.domain.post.repository.PostRepository;
import com.teamloci.loci.domain.user.User;
import com.teamloci.loci.global.error.CustomException;
import com.teamloci.loci.global.error.ErrorCode;
import com.teamloci.loci.global.util.RelationUtil;
import com.teamloci.loci.domain.friend.FriendshipRepository;
import com.teamloci.loci.domain.user.UserRepository;
import com.teamloci.loci.domain.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    private final PostCommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final FriendshipRepository friendshipRepository;
    private final NotificationService notificationService;

    private static final Pattern MENTION_PATTERN = Pattern.compile("@([a-z0-9._]+)");

    @Transactional
    public CommentDto.Response createComment(Long userId, Long postId, CommentDto.CreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        PostComment comment = PostComment.builder()
                .user(user)
                .post(post)
                .content(request.getContent())
                .build();
        PostComment savedComment = commentRepository.save(comment);

        Set<Long> mentionedUserIds = sendMentionNotifications(user, post, request.getContent());

        User postOwner = post.getUser();
        if (!postOwner.getId().equals(userId) && !mentionedUserIds.contains(postOwner.getId())) {
            String summary = request.getContent().length() > 20
                    ? request.getContent().substring(0, 20) + "..."
                    : request.getContent();

            notificationService.send(
                    postOwner,
                    NotificationType.POST_COMMENT,
                    "새로운 댓글",
                    user.getNickname() + "님: " + summary,
                    postId
            );
        }

        long friendCount = friendshipRepository.countFriends(userId);
        long postCount = postRepository.countByUserIdAndStatus(userId, PostStatus.ACTIVE);

        return CommentDto.Response.of(savedComment, "SELF", friendCount, postCount);
    }

    private Set<Long> sendMentionNotifications(User sender, Post post, String content) {
        Set<String> handles = new HashSet<>();
        Matcher matcher = MENTION_PATTERN.matcher(content);

        while (matcher.find()) {
            handles.add(matcher.group(1));
        }

        if (handles.isEmpty()) return Collections.emptySet();

        List<User> mentionedUsers = userRepository.findByHandleIn(new ArrayList<>(handles));
        Set<Long> notifiedUserIds = new HashSet<>();

        String summary = content.length() > 30
                ? content.substring(0, 30) + "..."
                : content;

        mentionedUsers.stream()
                .filter(u -> !u.getId().equals(sender.getId()))
                .forEach(target -> {
                    notificationService.send(
                            target,
                            NotificationType.COMMENT_MENTION,
                            "회원님을 언급했습니다",
                            sender.getNickname() + "님이 댓글에서 회원님을 언급했습니다: " + summary,
                            post.getId()
                    );
                    notifiedUserIds.add(target.getId());
                });

        return notifiedUserIds;
    }

    public CommentDto.ListResponse getComments(Long myUserId, Long postId, Long cursorId, int size) {
        if (!postRepository.existsById(postId)) {
            throw new CustomException(ErrorCode.POST_NOT_FOUND);
        }

        PageRequest pageable = PageRequest.of(0, size + 1);
        List<PostComment> comments = commentRepository.findByPostIdWithCursor(postId, cursorId, pageable);

        boolean hasNext = false;
        if (comments.size() > size) {
            hasNext = true;
            comments.remove(size);
        }
        Long nextCursor = comments.isEmpty() ? null : comments.get(comments.size() - 1).getId();

        Set<Long> authorIds = comments.stream()
                .map(c -> c.getUser().getId())
                .collect(Collectors.toSet());

        Set<Long> otherIds = authorIds.stream().filter(id -> !id.equals(myUserId)).collect(Collectors.toSet());
        Map<Long, Friendship> friendshipMap;
        if (otherIds.isEmpty()) {
            friendshipMap = Map.of();
        } else {
            friendshipMap = friendshipRepository.findAllRelationsBetween(myUserId, otherIds.stream().toList()).stream()
                    .collect(Collectors.toMap(
                            f -> f.getRequester().getId().equals(myUserId) ? f.getReceiver().getId() : f.getRequester().getId(),
                            f -> f
                    ));
        }

        Map<Long, Long> friendCountMap = new HashMap<>();
        Map<Long, Long> postCountMap = new HashMap<>();

        if (!authorIds.isEmpty()) {
            List<Long> authorIdList = new ArrayList<>(authorIds);

            friendshipRepository.countFriendsByUserIds(authorIdList).forEach(row ->
                    friendCountMap.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue())
            );

            postRepository.countPostsByUserIds(authorIdList, PostStatus.ACTIVE).forEach(row ->
                    postCountMap.put((Long) row[0], (Long) row[1])
            );
        }

        List<CommentDto.Response> commentDtos = comments.stream()
                .map(c -> {
                    Long userId = c.getUser().getId();

                    String status;
                    if (userId.equals(myUserId)) {
                        status = "SELF";
                    } else {
                        status = RelationUtil.resolveStatus(friendshipMap.get(userId), myUserId);
                    }

                    Long fCount = friendCountMap.getOrDefault(userId, 0L);
                    Long pCount = postCountMap.getOrDefault(userId, 0L);

                    return CommentDto.Response.of(c, status, fCount, pCount);
                })
                .collect(Collectors.toList());

        long totalCount = commentRepository.countByPostId(postId);

        return CommentDto.ListResponse.builder()
                .comments(commentDtos)
                .hasNext(hasNext)
                .nextCursor(nextCursor)
                .totalCount(totalCount)
                .build();
    }

    @Transactional
    public void deleteComment(Long userId, Long postId, Long commentId) {
        PostComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND));

        if (!comment.getPost().getId().equals(postId)) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        if (!comment.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        commentRepository.delete(comment);
    }

    private String resolveStatus(Friendship f, Long myUserId) {
        if (f == null) return "NONE";
        if (f.getStatus() == FriendshipStatus.FRIENDSHIP) return "FRIEND";

        if (f.getRequester().getId().equals(myUserId)) {
            return "PENDING_SENT";
        } else {
            return "PENDING_RECEIVED";
        }
    }
}