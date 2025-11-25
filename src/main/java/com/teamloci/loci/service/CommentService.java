package com.teamloci.loci.service;

import com.teamloci.loci.domain.*;
import com.teamloci.loci.dto.CommentDto;
import com.teamloci.loci.global.exception.CustomException;
import com.teamloci.loci.global.exception.code.ErrorCode;
import com.teamloci.loci.repository.FriendshipRepository;
import com.teamloci.loci.repository.PostCommentRepository;
import com.teamloci.loci.repository.PostRepository;
import com.teamloci.loci.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    private final PostCommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final FriendshipRepository friendshipRepository;

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

        return CommentDto.Response.of(savedComment, "SELF");
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
                .filter(id -> !id.equals(myUserId))
                .collect(Collectors.toSet());

        List<Friendship> friendships = friendshipRepository.findAllRelationsBetween(myUserId, authorIds.stream().toList());
        Map<Long, Friendship> friendshipMap = friendships.stream()
                .collect(Collectors.toMap(
                        f -> f.getRequester().getId().equals(myUserId) ? f.getReceiver().getId() : f.getRequester().getId(),
                        f -> f
                ));

        List<CommentDto.Response> commentDtos = comments.stream()
                .map(c -> {
                    String status;
                    if (c.getUser().getId().equals(myUserId)) {
                        status = "SELF";
                    } else {
                        status = resolveStatus(friendshipMap.get(c.getUser().getId()), myUserId);
                    }
                    return CommentDto.Response.of(c, status);
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
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));

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