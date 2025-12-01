package com.teamloci.loci.service;

import com.teamloci.loci.domain.*;
import com.teamloci.loci.global.exception.CustomException;
import com.teamloci.loci.global.exception.code.ErrorCode;
import com.teamloci.loci.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class ReactionService {

    private final PostReactionRepository postReactionRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final PostRepository postRepository;
    private final PostCommentRepository commentRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

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