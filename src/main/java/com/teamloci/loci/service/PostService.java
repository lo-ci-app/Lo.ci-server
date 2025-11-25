package com.teamloci.loci.service;

import com.teamloci.loci.domain.*;
import com.teamloci.loci.dto.PostDto;
import com.teamloci.loci.global.exception.CustomException;
import com.teamloci.loci.global.exception.code.ErrorCode;
import com.teamloci.loci.global.util.GeoUtils;
import com.teamloci.loci.repository.FriendshipRepository;
import com.teamloci.loci.repository.PostCommentRepository;
import com.teamloci.loci.repository.PostRepository;
import com.teamloci.loci.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final FriendshipRepository friendshipRepository;
    private final PostCommentRepository commentRepository;
    private final NotificationService notificationService;
    private final GeoUtils geoUtils;

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    private Post findPostById(Long postId) {
        return postRepository.findByIdWithDetails(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));
    }

    @Transactional
    public PostDto.PostDetailResponse createPost(Long authorId, PostDto.PostCreateRequest request) {
        User author = findUserById(authorId);

        String beaconId = geoUtils.latLngToBeaconId(request.getLatitude(), request.getLongitude());

        Post post = Post.builder()
                .user(author)
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .locationName(request.getLocationName())
                .beaconId(beaconId)
                .isArchived(request.getIsArchived())
                .build();

        if (request.getMediaList() != null) {
            request.getMediaList().forEach(mediaReq -> {
                post.addMedia(PostMedia.builder()
                        .mediaUrl(mediaReq.getMediaUrl())
                        .mediaType(mediaReq.getMediaType())
                        .sortOrder(mediaReq.getSortOrder())
                        .build());
            });
        }

        if (request.getCollaboratorIds() != null && !request.getCollaboratorIds().isEmpty()) {
            Set<Long> collaboratorIds = new HashSet<>(request.getCollaboratorIds());
            collaboratorIds.remove(authorId);

            if (!collaboratorIds.isEmpty()) {
                List<Friendship> relations = friendshipRepository.findAllRelationsBetween(authorId, collaboratorIds.stream().toList());

                Set<Long> validFriendIds = relations.stream()
                        .filter(f -> f.getStatus() == FriendshipStatus.FRIENDSHIP)
                        .map(f -> f.getRequester().getId().equals(authorId) ? f.getReceiver().getId() : f.getRequester().getId())
                        .collect(Collectors.toSet());

                if (!validFriendIds.containsAll(collaboratorIds)) {
                    throw new CustomException(ErrorCode.NOT_FRIENDS);
                }

                List<User> collaboratorUsers = userRepository.findAllById(collaboratorIds);
                collaboratorUsers.forEach(collaboratorUser ->
                        post.addCollaborator(PostCollaborator.builder()
                                .user(collaboratorUser)
                                .build())
                );
            }
        }

        Post savedPost = postRepository.save(post);

        // [알림] DB 저장 및 FCM 발송
        sendPostNotifications(author, savedPost);

        PostDto.PostDetailResponse response = PostDto.PostDetailResponse.from(findPostById(savedPost.getId()));
        response.getAuthor().setRelationStatus("SELF");
        return response;
    }

    public PostDto.PostDetailResponse getPost(Long postId, Long myUserId) {
        Post post = findPostById(postId);
        PostDto.PostDetailResponse response = PostDto.PostDetailResponse.from(post);

        enrichPostAuthors(List.of(response), myUserId);

        return response;
    }

    public PostDto.FeedResponse getPostsByUser(Long myUserId, Long targetUserId, Long cursorId, int size) {
        if (!userRepository.existsById(targetUserId)) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }
        Pageable pageable = PageRequest.of(0, size + 1);
        List<Post> posts = postRepository.findByUserIdWithCursor(targetUserId, cursorId, pageable);

        return makeFeedResponse(posts, size, myUserId);
    }

    @Transactional
    public void deletePost(Long currentUserId, Long postId) {
        Post post = findPostById(postId);

        if (!post.getUser().getId().equals(currentUserId)) {
            throw new CustomException(ErrorCode.NOT_POST_AUTHOR);
        }

        postRepository.delete(post);
    }

    @Transactional
    public PostDto.PostDetailResponse updatePost(Long currentUserId, Long postId, PostDto.PostCreateRequest request) {
        Post post = findPostById(postId);

        if (!post.getUser().getId().equals(currentUserId)) {
            throw new CustomException(ErrorCode.NOT_POST_AUTHOR);
        }

        String beaconId = geoUtils.latLngToBeaconId(request.getLatitude(), request.getLongitude());

        post.update(
                request.getLatitude(),
                request.getLongitude(),
                request.getLocationName(),
                beaconId,
                request.getIsArchived()
        );

        post.clearMedia();
        if (request.getMediaList() != null) {
            request.getMediaList().forEach(mediaReq -> {
                post.addMedia(PostMedia.builder()
                        .mediaUrl(mediaReq.getMediaUrl())
                        .mediaType(mediaReq.getMediaType())
                        .sortOrder(mediaReq.getSortOrder())
                        .build());
            });
        }

        post.clearCollaborators();
        if (request.getCollaboratorIds() != null && !request.getCollaboratorIds().isEmpty()) {
            Set<Long> collaboratorIds = new HashSet<>(request.getCollaboratorIds());
            List<User> collaboratorUsers = userRepository.findAllById(collaboratorIds);
            collaboratorUsers.forEach(collaboratorUser ->
                    post.addCollaborator(PostCollaborator.builder()
                            .user(collaboratorUser)
                            .build())
            );
        }

        return PostDto.PostDetailResponse.from(findPostById(post.getId()));
    }

    public List<PostDto.PostDetailResponse> getPostsByBeaconId(String beaconId, Long myUserId) {
        if (beaconId == null || beaconId.isBlank()) return List.of();

        List<Post> posts = postRepository.findByBeaconId(beaconId);
        List<PostDto.PostDetailResponse> responses = posts.stream()
                .map(PostDto.PostDetailResponse::from)
                .collect(Collectors.toList());

        enrichPostAuthors(responses, myUserId);
        return responses;
    }

    public List<PostDto.MapMarkerResponse> getMapMarkers(Double minLat, Double maxLat, Double minLon, Double maxLon) {
        List<Object[]> results = postRepository.findMapMarkers(minLat, maxLat, minLon, maxLon);

        return results.stream()
                .map(row -> {
                    String beaconId = (String) row[0];
                    Long count = ((Number) row[1]).longValue();
                    String thumbnail = (String) row[2];

                    GeoUtils.Pair<Double, Double> latLng = geoUtils.beaconIdToLatLng(beaconId);

                    if (latLng == null) return null;

                    return PostDto.MapMarkerResponse.builder()
                            .beaconId(beaconId)
                            .latitude(latLng.lat)
                            .longitude(latLng.lng)
                            .count(count)
                            .thumbnailImageUrl(thumbnail)
                            .build();
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public PostDto.FeedResponse getFriendFeed(Long myUserId, Long cursorId, int size) {
        Pageable pageable = PageRequest.of(0, size + 1);
        List<Post> posts = postRepository.findFriendPostsWithCursor(myUserId, cursorId, pageable);
        return makeFeedResponse(posts, size, myUserId);
    }

    private PostDto.FeedResponse makeFeedResponse(List<Post> posts, int size, Long myUserId) {
        boolean hasNext = false;
        if (posts.size() > size) {
            hasNext = true;
            posts.remove(size);
        }
        Long nextCursor = posts.isEmpty() ? null : posts.get(posts.size() - 1).getId();

        List<PostDto.PostDetailResponse> postDtos = posts.stream()
                .map(PostDto.PostDetailResponse::from)
                .collect(Collectors.toList());

        enrichPostAuthors(postDtos, myUserId);

        return PostDto.FeedResponse.builder()
                .posts(postDtos)
                .hasNext(hasNext)
                .nextCursor(nextCursor)
                .build();
    }

    private void enrichPostAuthors(List<PostDto.PostDetailResponse> posts, Long myUserId) {
        if (posts.isEmpty()) return;

        Set<Long> targetUserIds = new HashSet<>();
        List<Long> postIds = new ArrayList<>();

        for (PostDto.PostDetailResponse p : posts) {
            postIds.add(p.getId());

            if (!p.getAuthor().getId().equals(myUserId)) {
                targetUserIds.add(p.getAuthor().getId());
            }
            if (p.getCollaborators() != null) {
                p.getCollaborators().stream()
                        .map(PostDto.UserSimpleResponse::getId)
                        .filter(id -> !id.equals(myUserId))
                        .forEach(targetUserIds::add);
            }
        }

        Map<Long, Long> commentCountMap = commentRepository.countByPostIdIn(postIds).stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));

        Map<Long, Friendship> friendshipMap;
        if (targetUserIds.isEmpty()) {
            friendshipMap = Map.of();
        } else {
            List<Friendship> friendships = friendshipRepository.findAllRelationsBetween(myUserId, targetUserIds.stream().toList());
            friendshipMap = friendships.stream()
                    .collect(Collectors.toMap(
                            f -> f.getRequester().getId().equals(myUserId) ? f.getReceiver().getId() : f.getRequester().getId(),
                            f -> f
                    ));
        }

        for (PostDto.PostDetailResponse p : posts) {
            p.setCommentCount(commentCountMap.getOrDefault(p.getId(), 0L));

            setStatus(p.getAuthor(), myUserId, friendshipMap);

            if (p.getCollaborators() != null) {
                for (PostDto.UserSimpleResponse collaborator : p.getCollaborators()) {
                    setStatus(collaborator, myUserId, friendshipMap);
                }
            }
        }
    }

    private void setStatus(PostDto.UserSimpleResponse userRes, Long myUserId, Map<Long, Friendship> friendshipMap) {
        if (userRes.getId().equals(myUserId)) {
            userRes.setRelationStatus("SELF");
        } else {
            Friendship f = friendshipMap.get(userRes.getId());
            userRes.setRelationStatus(resolveStatus(f, myUserId));
        }
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

    private void sendPostNotifications(User author, Post post) {
        try {
            post.getCollaborators().stream()
                    .map(PostCollaborator::getUser)
                    .filter(u -> !u.getId().equals(author.getId()))
                    .forEach(taggedUser -> {
                        notificationService.send(
                                taggedUser,
                                NotificationType.POST_TAGGED,
                                "함께한 순간",
                                author.getNickname() + "님이 회원님을 게시물에 태그했습니다.",
                                post.getId()
                        );
                    });

            List<Friendship> friendships = friendshipRepository.findAllFriendsWithUsers(author.getId());
            List<User> friends = friendships.stream()
                    .map(f -> f.getRequester().getId().equals(author.getId()) ? f.getReceiver() : f.getRequester())
                    .filter(u -> u.getStatus() == UserStatus.ACTIVE)
                    .collect(Collectors.toList());

            if (!friends.isEmpty()) {
                notificationService.sendMulticast(
                        friends,
                        NotificationType.NEW_POST,
                        "새로운 Loci!",
                        author.getNickname() + "님이 지금 순간을 공유했어요 📸",
                        post.getId()
                );
            }
        } catch (Exception e) {
            log.error("게시글 작성 알림 발송 실패: {}", e.getMessage());
        }
    }
}