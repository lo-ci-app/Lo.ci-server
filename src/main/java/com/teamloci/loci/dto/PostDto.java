package com.teamloci.loci.dto;

import com.teamloci.loci.domain.MediaType;
import com.teamloci.loci.domain.Post;
import com.teamloci.loci.domain.PostCollaborator;
import com.teamloci.loci.domain.PostMedia;
import com.teamloci.loci.domain.User;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Schema(description = "게시물 관련 DTO")
public class PostDto {

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "포스트 응답에 사용될 간략한 유저 정보")
    public static class UserSimpleResponse {
        @Schema(description = "유저 ID", example = "1")
        private Long id;
        @Schema(description = "닉네임", example = "즐거운판다")
        private String nickname;
        @Schema(description = "프로필 이미지 URL", example = "https://fiv5.../profile.png")
        private String profileUrl;

        public static UserSimpleResponse from(User user) {
            return new UserSimpleResponse(
                    user.getId(),
                    user.getNickname(),
                    user.getProfileUrl()
            );
        }
    }

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "포스트 미디어 응답")
    public static class MediaResponse {
        @Schema(description = "미디어 ID", example = "1")
        private Long id;
        @Schema(description = "S3 미디어 URL", example = "https://fiv5.../media.mp4")
        private String mediaUrl;
        @Schema(description = "미디어 타입 (IMAGE 또는 VIDEO)", example = "VIDEO")
        private MediaType mediaType;
        @Schema(description = "정렬 순서 (낮을수록 먼저)", example = "1")
        private int sortOrder;

        public static MediaResponse from(PostMedia media) {
            return new MediaResponse(
                    media.getId(),
                    media.getMediaUrl(),
                    media.getMediaType(),
                    media.getSortOrder()
            );
        }
    }

    @Getter
    @NoArgsConstructor
    @Schema(description = "포스트 생성/수정 시 미디어 정보 요청")
    public static class MediaRequest {
        @Schema(description = "S3에 업로드된 미디어 URL", example = "https://fiv5.../media.mp4")
        @NotEmpty
        private String mediaUrl;

        @Schema(description = "미디어 타입 (IMAGE 또는 VIDEO)", example = "VIDEO")
        @NotNull
        private MediaType mediaType;

        @Schema(description = "정렬 순서 (낮을수록 먼저)", example = "1")
        @NotNull
        private Integer sortOrder;
    }

    @Getter
    @NoArgsConstructor
    @Schema(description = "포스트 생성/수정 요청 Body")
    public static class PostCreateRequest {
        @Schema(description = "미디어 목록")
        private List<MediaRequest> mediaList;
        @Schema(description = "공동 작업자 User ID 목록")
        private List<Long> collaboratorIds;
        @NotNull
        private Double latitude;
        @NotNull
        private Double longitude;
        private String locationName;
        private Boolean isAutoArchive;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "포스트 상세 조회 응답")
    public static class PostDetailResponse {
        @Schema(description = "포스트 ID", example = "1")
        private Long id;
        @Schema(description = "위도", example = "37.5665")
        private Double latitude;
        @Schema(description = "경도", example = "126.9780")
        private Double longitude;
        @Schema(description = "장소명", example = "스타벅스 강남점")
        private String locationName;
        @Schema(description = "작성자 정보")
        private UserSimpleResponse author;
        @Schema(description = "미디어 목록")
        private List<MediaResponse> mediaList;
        @Schema(description = "공동 작업자 목록")
        private List<UserSimpleResponse> collaborators;
        @Schema(description = "생성 시간")
        private LocalDateTime createdAt;
        @Schema(description = "마지막 수정 시간")
        private LocalDateTime updatedAt;
        @Schema(description = "30일 후 자동 보관 여부")
        private boolean isAutoArchive;

        public static PostDetailResponse from(Post post) {
            return PostDetailResponse.builder()
                    .id(post.getId())
                    .latitude(post.getLatitude())
                    .longitude(post.getLongitude())
                    .locationName(post.getLocationName())
                    .author(UserSimpleResponse.from(post.getUser()))
                    .mediaList(post.getMediaList().stream()
                            .map(MediaResponse::from)
                            .collect(Collectors.toList()))
                    .collaborators(post.getCollaborators().stream()
                            .map(PostCollaborator::getUser)
                            .map(UserSimpleResponse::from)
                            .collect(Collectors.toList()))
                    .createdAt(post.getCreatedAt())
                    .updatedAt(post.getUpdatedAt())
                    .isAutoArchive(post.isAutoArchive())
                    .build();
        }
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "지도 마커(비콘 요약) 응답")
    public static class MapMarkerResponse {
        @Schema(description = "비콘 ID (육각형 구역 ID)", example = "89283082807ffff")
        private String beaconId;

        @Schema(description = "비콘 중심 위도", example = "37.5665")
        private Double latitude;

        @Schema(description = "비콘 중심 경도", example = "126.9780")
        private Double longitude;

        @Schema(description = "해당 구역의 게시글 총 개수", example = "5")
        private Long count;

        @Schema(description = "대표 썸네일 이미지 URL (가장 최신 글)", example = "https://fiv5.../thumb.jpg")
        private String thumbnailImageUrl;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "피드(무한 스크롤) 응답")
    public static class FeedResponse {
        @Schema(description = "포스트 목록")
        private List<PostDetailResponse> posts;

        @Schema(description = "다음 페이지가 있는지 여부")
        private boolean hasNext;

        @Schema(description = "다음 요청에 사용할 커서 (마지막 포스트의 작성 시간)", example = "2025-11-20T10:00:00")
        private LocalDateTime nextCursor;
    }
}