package com.teamloci.loci.service;

import com.teamloci.loci.domain.*;
import com.teamloci.loci.domain.Post;
import com.teamloci.loci.domain.PostCollaborator;
import com.teamloci.loci.domain.PostMedia;
import com.teamloci.loci.domain.User;
import com.teamloci.loci.dto.PostDto;
import com.teamloci.loci.global.exception.CustomException;
import com.teamloci.loci.global.exception.code.ErrorCode;
import com.teamloci.loci.global.util.GeoUtils;
import com.teamloci.loci.repository.PostRepository;
import com.teamloci.loci.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
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
                .contents(request.getContents())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .locationName(request.getLocationName())
                .beaconId(beaconId)
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

        return PostDto.PostDetailResponse.from(findPostById(savedPost.getId()));
    }

    public PostDto.PostDetailResponse getPost(Long postId) {
        Post post = findPostById(postId);
        return PostDto.PostDetailResponse.from(post);
    }

    public List<PostDto.PostDetailResponse> getPostsByUser(Long userId) {
        List<Post> posts = postRepository.findByUserIdWithUser(userId);

        return posts.stream()
                .map(post -> getPost(post.getId()))
                .collect(Collectors.toList());
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
                request.getContents(),
                request.getLatitude(),
                request.getLongitude(),
                request.getLocationName(),
                beaconId
        );

        post.updateContents(request.getContents());

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

    public List<PostDto.PostDetailResponse> getPostsByLocation(Double latitude, Double longitude) {
        String beaconId = geoUtils.latLngToBeaconId(longitude, latitude);

        if (beaconId == null) {
            return List.of();
        }

        List<Post> posts = postRepository.findByBeaconId(beaconId);

        return posts.stream()
                .map(PostDto.PostDetailResponse::from)
                .collect(Collectors.toList());
    }
}