package com.teamloci.loci.service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.teamloci.loci.domain.*;
import com.teamloci.loci.repository.FriendshipRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teamloci.loci.dto.PostDto;
import com.teamloci.loci.global.exception.CustomException;
import com.teamloci.loci.global.exception.code.ErrorCode;
import com.teamloci.loci.global.util.GeoUtils;
import com.teamloci.loci.repository.PostRepository;
import com.teamloci.loci.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final FriendshipRepository friendshipRepository;
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
            List<User> collaboratorUsers = userRepository.findAllById(collaboratorIds);

            if (collaboratorUsers.size() != collaboratorIds.size()) {
                throw new CustomException(ErrorCode.USER_NOT_FOUND);
            }

            collaboratorUsers.forEach(collaboratorUser ->
                    post.addCollaborator(PostCollaborator.builder()
                            .user(collaboratorUser)
                            .build())
            );
        }

        Post savedPost = postRepository.save(post);

        PostDto.PostDetailResponse response = PostDto.PostDetailResponse.from(findPostById(savedPost.getId()));
        response.getAuthor().setRelationStatus("SELF");
        return response;
    }

    public PostDto.PostDetailResponse getPost(Long postId, Long myUserId) {
        Post post = findPostById(postId);
        PostDto.PostDetailResponse response = PostDto.PostDetailResponse.from(post);
        enrichSinglePostAuthor(response, myUserId);
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

            if (collaboratorUsers.size() != collaboratorIds.size()) {
                throw new CustomException(ErrorCode.USER_NOT_FOUND);
            }

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
                .filter(java.util.Objects::nonNull)
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

        Set<Long> authorIds = posts.stream()
                .map(p -> p.getAuthor().getId())
                .filter(id -> !id.equals(myUserId))
                .collect(Collectors.toSet());

        if (authorIds.isEmpty()) {
            posts.forEach(p -> {
                if (p.getAuthor().getId().equals(myUserId)) p.getAuthor().setRelationStatus("SELF");
            });
            return;
        }

        List<Friendship> friendships = friendshipRepository.findAllRelationsBetween(myUserId, authorIds.stream().toList());

        Map<Long, Friendship> friendshipMap = friendships.stream()
                .collect(Collectors.toMap(
                        f -> f.getRequester().getId().equals(myUserId) ? f.getReceiver().getId() : f.getRequester().getId(),
                        f -> f
                ));

        for (PostDto.PostDetailResponse p : posts) {
            Long authorId = p.getAuthor().getId();
            if (authorId.equals(myUserId)) {
                p.getAuthor().setRelationStatus("SELF");
            } else {
                Friendship f = friendshipMap.get(authorId);
                p.getAuthor().setRelationStatus(resolveStatus(f, myUserId));
            }
        }
    }

    private void enrichSinglePostAuthor(PostDto.PostDetailResponse post, Long myUserId) {
        Long authorId = post.getAuthor().getId();
        if (authorId.equals(myUserId)) {
            post.getAuthor().setRelationStatus("SELF");
            return;
        }
        Friendship f = friendshipRepository.findFriendshipBetween(myUserId, authorId).orElse(null);
        post.getAuthor().setRelationStatus(resolveStatus(f, myUserId));
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