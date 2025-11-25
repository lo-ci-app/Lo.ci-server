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
    private final PostCommentRepository commentRepository; // 댓글 수 조회를 위해 추가
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

        // 공동 작업자 검증 및 추가
        if (request.getCollaboratorIds() != null && !request.getCollaboratorIds().isEmpty()) {
            Set<Long> collaboratorIds = new HashSet<>(request.getCollaboratorIds());
            collaboratorIds.remove(authorId); // 본인 제외

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

        // 알림 발송
        sendPostNotifications(author, savedPost);

        PostDto.PostDetailResponse response = PostDto.PostDetailResponse.from(findPostById(savedPost.getId()));
        response.getAuthor().setRelationStatus("SELF");
        return response;
    }

    public PostDto.PostDetailResponse getPost(Long postId, Long myUserId) {
        Post post = findPostById(postId);
        PostDto.PostDetailResponse response = PostDto.PostDetailResponse.from(post);

        // 단일 조회도 리스트 처리 로직을 재사용하여 '댓글 수'와 '관계'를 모두 채움
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
        // 수정 시 공동 작업자 검증 로직도 추가 가능 (현재는 단순 추가만 구현됨)
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

    // [핵심 메서드] 포스트 목록에 '친구 관계'와 '댓글 수'를 채워 넣는 로직
    private void enrichPostAuthors(List<PostDto.PostDetailResponse> posts, Long myUserId) {
        if (posts.isEmpty()) return;

        Set<Long> targetUserIds = new HashSet<>();
        List<Long> postIds = new ArrayList<>();

        // 1. 조회할 ID 수집
        for (PostDto.PostDetailResponse p : posts) {
            postIds.add(p.getId()); // 댓글 수 조회용

            // 작성자 ID (나 제외)
            if (!p.getAuthor().getId().equals(myUserId)) {
                targetUserIds.add(p.getAuthor().getId());
            }
            // 공동 작업자 ID (나 제외)
            if (p.getCollaborators() != null) {
                p.getCollaborators().stream()
                        .map(PostDto.UserSimpleResponse::getId)
                        .filter(id -> !id.equals(myUserId))
                        .forEach(targetUserIds::add);
            }
        }

        // 2. 댓글 수 일괄 조회 (Batch)
        Map<Long, Long> commentCountMap = commentRepository.countByPostIdIn(postIds).stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));

        // 3. 친구 관계 일괄 조회 (Batch)
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

        // 4. 데이터 주입
        for (PostDto.PostDetailResponse p : posts) {
            // 댓글 수 설정
            p.setCommentCount(commentCountMap.getOrDefault(p.getId(), 0L));

            // 작성자 관계 설정
            setStatus(p.getAuthor(), myUserId, friendshipMap);

            // 공동 작업자 관계 설정
            if (p.getCollaborators() != null) {
                for (PostDto.UserSimpleResponse collaborator : p.getCollaborators()) {
                    setStatus(collaborator, myUserId, friendshipMap);
                }
            }
        }
    }

    // [헬퍼 메서드] UserSimpleResponse에 relationStatus 주입
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

    // [헬퍼 메서드] 알림 발송
    private void sendPostNotifications(User author, Post post) {
        try {
            List<Friendship> friendships = friendshipRepository.findAllFriendsWithUsers(author.getId());

            List<String> fcmTokens = friendships.stream()
                    .map(f -> f.getRequester().getId().equals(author.getId()) ? f.getReceiver() : f.getRequester())
                    .filter(u -> u.getStatus() == UserStatus.ACTIVE)
                    .map(User::getFcmToken)
                    .filter(token -> token != null && !token.isBlank())
                    .collect(Collectors.toList());

            if (!fcmTokens.isEmpty()) {
                // [TODO] 알림 기능 구현 후 주석 해제
                // notificationService.sendPostCreationNotification(fcmTokens, author.getNickname(), post.getId());
            }
        } catch (Exception e) {
            log.error("게시글 작성 알림 발송 실패: {}", e.getMessage());
        }
    }
}