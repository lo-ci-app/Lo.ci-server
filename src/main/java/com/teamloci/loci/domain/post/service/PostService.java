package com.teamloci.loci.domain.post.service;

import com.teamloci.loci.domain.friend.Friendship;
import com.teamloci.loci.domain.friend.FriendshipRepository;
import com.teamloci.loci.domain.friend.FriendshipStatus;
import com.teamloci.loci.domain.intimacy.entity.FriendshipIntimacy;
import com.teamloci.loci.domain.intimacy.service.IntimacyService;
import com.teamloci.loci.domain.post.dto.PostDto;
import com.teamloci.loci.domain.post.entity.*;
import com.teamloci.loci.domain.post.event.PostCreatedEvent;
import com.teamloci.loci.domain.post.repository.PostCommentRepository;
import com.teamloci.loci.domain.post.repository.PostReactionRepository;
import com.teamloci.loci.domain.post.repository.PostRepository;
import com.teamloci.loci.domain.stat.repository.UserBeaconStatsRepository;
import com.teamloci.loci.domain.user.User;
import com.teamloci.loci.domain.user.UserDto;
import com.teamloci.loci.domain.user.UserRepository;
import com.teamloci.loci.domain.user.UserActivityService;
import com.teamloci.loci.global.error.CustomException;
import com.teamloci.loci.global.error.ErrorCode;
import com.teamloci.loci.global.util.GeoUtils;
import com.teamloci.loci.global.util.RelationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
    private final PostReactionRepository reactionRepository;
    private final GeoUtils geoUtils;
    private final UserActivityService userActivityService;
    private final IntimacyService intimacyService;
    private final ApplicationEventPublisher eventPublisher;
    private final UserBeaconStatsRepository userBeaconStatsRepository;

    @Value("${feature.use-new-map-marker:false}")
    private boolean useNewMapMarker;

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

        String thumbnailUrl = null;
        if (request.getMediaList() != null && !request.getMediaList().isEmpty()) {
            thumbnailUrl = request.getMediaList().stream()
                    .min(Comparator.comparingInt(PostDto.MediaRequest::getSortOrder))
                    .map(PostDto.MediaRequest::getMediaUrl)
                    .orElse(null);
        }

        Post post = Post.builder()
                .user(author)
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .locationName(request.getLocationName())
                .beaconId(beaconId)
                .thumbnailUrl(thumbnailUrl)
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

        eventPublisher.publishEvent(new PostCreatedEvent(savedPost));

        PostDto.PostDetailResponse response = PostDto.PostDetailResponse.from(findPostById(savedPost.getId()));

        enrichPostUserData(List.of(response), authorId);

        return response;
    }

    public PostDto.PostDetailResponse getPost(Long postId, Long myUserId) {
        Post post = findPostById(postId);
        PostDto.PostDetailResponse response = PostDto.PostDetailResponse.from(post);

        enrichPostUserData(List.of(response), myUserId);

        return response;
    }

    public PostDto.FeedResponse getPostsByUser(Long myUserId, Long targetUserId, Long cursorId, int size) {
        if (!userRepository.existsById(targetUserId)) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }

        Pageable pageable = PageRequest.of(0, size + 1);
        List<Post> posts;

        if (myUserId.equals(targetUserId)) {
            posts = postRepository.findByUserIdAndStatusInWithCursor(
                    targetUserId,
                    List.of(PostStatus.ACTIVE, PostStatus.ARCHIVED),
                    cursorId,
                    pageable
            );
        } else {
            posts = postRepository.findByUserIdAndStatusInWithCursor(
                    targetUserId,
                    List.of(PostStatus.ACTIVE),
                    cursorId,
                    pageable
            );
        }

        return makeFeedResponse(posts, size, myUserId);
    }

    public PostDto.FeedResponse getArchivedPosts(Long userId, Long cursorId, int size) {
        Pageable pageable = PageRequest.of(0, size + 1);
        List<Post> posts = postRepository.findArchivedPostsByUserIdWithCursor(userId, cursorId, pageable);
        return makeFeedResponse(posts, size, userId);
    }

    @Transactional
    public void deletePost(Long currentUserId, Long postId) {
        Post post = findPostById(postId);

        if (!post.getUser().getId().equals(currentUserId)) {
            throw new CustomException(ErrorCode.NOT_POST_AUTHOR);
        }

        String beaconId = post.getBeaconId();
        Long userId = post.getUser().getId();

        postRepository.delete(post);

        syncUserBeaconStats(userId, beaconId);
    }

    @Transactional
    public void archivePost(Long userId, Long postId) {
        Post post = findPostById(postId);
        if (!post.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.NOT_POST_AUTHOR);
        }
        post.archive();
    }

    @Transactional
    public void unarchivePost(Long userId, Long postId) {
        Post post = findPostById(postId);
        if (!post.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.NOT_POST_AUTHOR);
        }
        post.restore();
    }

    @Transactional
    public PostDto.PostDetailResponse updatePost(Long currentUserId, Long postId, PostDto.PostCreateRequest request) {
        Post post = findPostById(postId);

        if (!post.getUser().getId().equals(currentUserId)) {
            throw new CustomException(ErrorCode.NOT_POST_AUTHOR);
        }

        String beaconId = geoUtils.latLngToBeaconId(request.getLatitude(), request.getLongitude());

        String thumbnailUrl = null;
        if (request.getMediaList() != null && !request.getMediaList().isEmpty()) {
            thumbnailUrl = request.getMediaList().stream()
                    .min(Comparator.comparingInt(PostDto.MediaRequest::getSortOrder))
                    .map(PostDto.MediaRequest::getMediaUrl)
                    .orElse(null);
        }

        post.update(
                request.getLatitude(),
                request.getLongitude(),
                request.getLocationName(),
                beaconId,
                thumbnailUrl
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

        List<User> friends = friendshipRepository.findActiveFriendsByUserId(myUserId);
        List<Long> friendIds = friends.stream().map(User::getId).collect(Collectors.toList());

        if (friendIds.isEmpty()) {
            friendIds.add(-1L);
        }

        List<Post> posts = postRepository.findTimelinePosts(beaconId, myUserId, friendIds);

        List<PostDto.PostDetailResponse> responses = posts.stream()
                .map(PostDto.PostDetailResponse::from)
                .collect(Collectors.toList());

        enrichPostUserData(responses, myUserId);
        return responses;
    }

    public List<PostDto.MapMarkerResponse> getMapMarkers(Double minLat, Double maxLat, Double minLon, Double maxLon, Long myUserId) {
        if (useNewMapMarker) {
            try {
                return getMapMarkersOptimized(minLat, maxLat, minLon, maxLon, myUserId);
            } catch (DataAccessException e) {
                log.warn("[Map Optimization DB Error] 기존 로직으로 대체 실행", e);
            } catch (Exception e) {
                log.error("[Map Optimization Code Error] 로직 버그 발생! 기존 로직으로 대체", e);
            }
        }

        List<Object[]> results = postRepository.findMapMarkers(minLat, maxLat, minLon, maxLon, myUserId);
        return mapToMarkerResponse(results);
    }

    private List<PostDto.MapMarkerResponse> getMapMarkersOptimized(Double minLat, Double maxLat, Double minLon, Double maxLon, Long myUserId) {
        List<User> friends = friendshipRepository.findActiveFriendsByUserId(myUserId);
        List<Long> friendIds = friends.stream().map(User::getId).collect(Collectors.toList());
        friendIds.add(myUserId);

        List<Object[]> results = userBeaconStatsRepository.findMarkersByFriendsInArea(
                friendIds, minLat, maxLat, minLon, maxLon
        );

        return results.stream()
                .map(row -> {
                    String beaconId = (String) row[0];
                    Long count = ((Number) row[1]).longValue();
                    String thumbnail = (String) row[2];
                    java.sql.Timestamp ts = (java.sql.Timestamp) row[3];
                    LocalDateTime latestAt = ts != null ? ts.toLocalDateTime() : null;

                    GeoUtils.Pair<Double, Double> latLng = geoUtils.beaconIdToLatLng(beaconId);
                    if (latLng == null) return null;

                    return PostDto.MapMarkerResponse.builder()
                            .beaconId(beaconId)
                            .latitude(latLng.lat)
                            .longitude(latLng.lng)
                            .count(count)
                            .thumbnailImageUrl(thumbnail)
                            .latestPostedAt(latestAt)
                            .build();
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private List<PostDto.MapMarkerResponse> mapToMarkerResponse(List<Object[]> results) {
        return results.stream()
                .map(row -> {
                    String beaconId = (String) row[0];
                    Long count = ((Number) row[1]).longValue();
                    String thumbnail = (String) row[2];
                    java.sql.Timestamp ts = (java.sql.Timestamp) row[3];
                    LocalDateTime latestAt = ts != null ? ts.toLocalDateTime() : null;

                    GeoUtils.Pair<Double, Double> latLng = geoUtils.beaconIdToLatLng(beaconId);

                    if (latLng == null) return null;

                    return PostDto.MapMarkerResponse.builder()
                            .beaconId(beaconId)
                            .latitude(latLng.lat)
                            .longitude(latLng.lng)
                            .count(count)
                            .thumbnailImageUrl(thumbnail)
                            .latestPostedAt(latestAt)
                            .build();
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public List<PostDto.FriendMapMarkerResponse> getFriendMapMarkers(Long myUserId) {
        List<User> friends = friendshipRepository.findActiveFriendsByUserId(myUserId);
        if (friends.isEmpty()) {
            return List.of();
        }
        List<Long> friendIds = friends.stream().map(User::getId).toList();

        List<Post> posts = postRepository.findLatestPostsByUserIds(friendIds);

        return posts.stream()
                .map(p -> {
                    UserDto.UserResponse userResp = UserDto.UserResponse.from(p.getUser());
                    userResp.setRelationStatus("FRIEND");

                    return PostDto.FriendMapMarkerResponse.builder()
                            .user(userResp)
                            .beacon(PostDto.BeaconInfo.builder()
                                    .id(p.getBeaconId())
                                    .latitude(p.getLatitude())
                                    .longitude(p.getLongitude())
                                    .build())
                            .post(PostDto.PostInfo.builder()
                                    .id(p.getId())
                                    .thumbnailUrl(p.getThumbnailUrl())
                                    .createdAt(p.getCreatedAt())
                                    .build())
                            .build();
                })
                .collect(Collectors.toList());
    }

    public PostDto.FeedResponse getFriendFeed(Long myUserId, Long cursorId, int size) {

        List<User> friends = friendshipRepository.findActiveFriendsByUserId(myUserId);

        List<Long> targetUserIds = new ArrayList<>();
        targetUserIds.add(myUserId);

        for (User friend : friends) {
            targetUserIds.add(friend.getId());
        }

        Pageable pageable = PageRequest.of(0, size + 1);
        List<Post> posts = postRepository.findByUserIdInWithCursor(targetUserIds, cursorId, pageable);

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

        enrichPostUserData(postDtos, myUserId);

        return PostDto.FeedResponse.builder()
                .posts(postDtos)
                .hasNext(hasNext)
                .nextCursor(nextCursor)
                .build();
    }

    private void enrichPostUserData(List<PostDto.PostDetailResponse> posts, Long myUserId) {
        if (posts.isEmpty()) return;

        Set<Long> targetUserIdsSet = new HashSet<>();
        List<Long> postIds = new ArrayList<>();

        for (PostDto.PostDetailResponse p : posts) {
            postIds.add(p.getId());
            targetUserIdsSet.add(p.getUser().getId());
            if (p.getCollaborators() != null) {
                p.getCollaborators().forEach(c -> targetUserIdsSet.add(c.getId()));
            }
        }
        List<Long> targetUserIds = new ArrayList<>(targetUserIdsSet);

        Map<Long, Map<ReactionType, Long>> reactionCounts = new HashMap<>();
        reactionRepository.countReactionsByPostIds(postIds).forEach(row -> {
            Long pid = (Long) row[0];
            ReactionType type = (ReactionType) row[1];
            Long count = (Long) row[2];
            reactionCounts.computeIfAbsent(pid, k -> new HashMap<>()).put(type, count);
        });

        Map<Long, ReactionType> myReactions = new HashMap<>();
        reactionRepository.findMyReactions(postIds, myUserId).forEach(row -> {
            myReactions.put((Long) row[0], (ReactionType) row[1]);
        });

        Set<Long> otherUserIds = targetUserIds.stream()
                .filter(id -> !id.equals(myUserId))
                .collect(Collectors.toSet());

        Map<Long, Friendship> friendshipMap;
        if (otherUserIds.isEmpty()) {
            friendshipMap = Map.of();
        } else {
            friendshipMap = friendshipRepository.findAllRelationsBetween(myUserId, otherUserIds.stream().toList()).stream()
                    .collect(Collectors.toMap(
                            f -> f.getRequester().getId().equals(myUserId) ? f.getReceiver().getId() : f.getRequester().getId(),
                            f -> f
                    ));
        }

        Map<Long, UserActivityService.UserStats> statsMap = userActivityService.getUserStatsMap(new ArrayList<>(targetUserIds));
        Map<Long, FriendshipIntimacy> myIntimacyMap = intimacyService.getIntimacyMap(myUserId, targetUserIds);

        for (PostDto.PostDetailResponse p : posts) {
            Map<ReactionType, Long> postReactions = reactionCounts.getOrDefault(p.getId(), Collections.emptyMap());

            p.setReactions(new PostDto.ReactionSummary(
                    myReactions.get(p.getId()),
                    postReactions
            ));

            fillUserInfo(p.getUser(), myUserId, friendshipMap, statsMap, myIntimacyMap);

            if (p.getCollaborators() != null) {
                p.getCollaborators().forEach(c ->
                        fillUserInfo(c, myUserId, friendshipMap, statsMap, myIntimacyMap)
                );
            }
        }
    }

    private void fillUserInfo(UserDto.UserResponse userRes, Long myUserId,
                              Map<Long, Friendship> friendshipMap,
                              Map<Long, UserActivityService.UserStats> statsMap,
                              Map<Long, FriendshipIntimacy> intimacyMap) {

        String relationStatus = "NONE";
        if (userRes.getId().equals(myUserId)) {
            relationStatus = "SELF";
        } else {
            Friendship f = friendshipMap.get(userRes.getId());
            relationStatus = RelationUtil.resolveStatus(f, myUserId);
        }
        userRes.setRelationStatus(relationStatus);

        UserActivityService.UserStats stats = statsMap.getOrDefault(userRes.getId(), new UserActivityService.UserStats(0,0,0,0,0));

        userRes.setFriendCount(stats.friendCount());
        userRes.setPostCount(stats.postCount());
        userRes.setStreakCount(stats.streakCount());
        userRes.setVisitedPlaceCount(stats.visitedPlaceCount());

        userRes.applyIntimacyInfo(intimacyMap.get(userRes.getId()), stats.totalIntimacyLevel());
    }

    public List<PostDto.VisitedPlaceResponse> getVisitedPlaces(Long userId, int page, int size) {
        int offset = page * size;

        List<Object[]> results = postRepository.findVisitedPlacesByUserId(userId, size, offset);

        return results.stream()
                .map(row -> {
                    String beaconId = (String) row[0];

                    GeoUtils.Pair<Double, Double> latLng = geoUtils.beaconIdToLatLng(beaconId);

                    java.sql.Timestamp timestamp = (java.sql.Timestamp) row[4];

                    return PostDto.VisitedPlaceResponse.builder()
                            .beaconId(beaconId)
                            .latitude(latLng != null ? latLng.lat : null)
                            .longitude(latLng != null ? latLng.lng : null)
                            .locationName((String) row[1])
                            .postCount(((Number) row[2]).longValue())
                            .thumbnailUrl((String) row[3])
                            .lastVisitedAt(timestamp != null ? timestamp.toLocalDateTime() : null)
                            .build();
                })
                .collect(Collectors.toList());
    }

    public PostDto.FriendVisitResponse checkFriendFootprints(Long myUserId, Double latitude, Double longitude, int size) {
        String centerBeaconId = geoUtils.latLngToBeaconId(latitude, longitude);
        List<String> targetBeaconIds = geoUtils.getHexagonNeighbors(centerBeaconId);

        boolean isVisitedByMe = postRepository.existsByBeaconIdInAndUserId(targetBeaconIds, myUserId);

        List<User> friends = friendshipRepository.findActiveFriendsByUserId(myUserId);
        if (friends.isEmpty()) {
            return PostDto.FriendVisitResponse.builder()
                    .isVisitedByMe(isVisitedByMe)
                    .visitors(List.of())
                    .totalCount(0L)
                    .build();
        }
        List<Long> friendIds = friends.stream().map(User::getId).toList();

        PageRequest pageRequest = PageRequest.of(0, size);
        List<User> visitors = postRepository.findFriendsInBeacons(targetBeaconIds, friendIds, pageRequest);

        if (visitors.isEmpty()) {
            return PostDto.FriendVisitResponse.builder()
                    .isVisitedByMe(isVisitedByMe)
                    .visitors(List.of())
                    .totalCount(0L)
                    .build();
        }

        Long totalCount = postRepository.countFriendsInBeacons(targetBeaconIds, friendIds);

        return PostDto.FriendVisitResponse.builder()
                .isVisitedByMe(isVisitedByMe)
                .visitors(visitors.stream().map(UserDto.UserResponse::from).toList())
                .totalCount(totalCount)
                .build();
    }

    private void syncUserBeaconStats(Long userId, String beaconId) {
        long count = postRepository.countByUserIdAndBeaconId(userId, beaconId);

        userBeaconStatsRepository.findByUserIdAndBeaconId(userId, beaconId)
                .ifPresent(stats -> {
                    if (count <= 0) {
                        userBeaconStatsRepository.delete(stats);
                    } else {
                        postRepository.findTopByUserIdAndBeaconIdOrderByIdDesc(userId, beaconId)
                                .ifPresent(latestPost -> {
                                    stats.sync(count, latestPost.getThumbnailUrl(), latestPost.getCreatedAt());
                                });
                    }
                });
    }
}