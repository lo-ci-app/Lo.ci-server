package com.teamloci.loci.domain.post.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonProperty;

import com.teamloci.loci.domain.post.entity.*;
import com.teamloci.loci.domain.user.UserDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Schema(description = "게시물 관련 DTO")
public class PostDto {

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

        @Schema(description = "30일 후 자동 보관 여부 설정 (true: 보관함 이동, false: 영구 게시). 미입력 시 기본값 true", example = "true")
        @JsonProperty("isArchived")
        private Boolean isArchived;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "반응(이모지) 요약 정보")
    public static class ReactionSummary {
        @Schema(description = "내가 누른 반응 타입 (없으면 null)", example = "LIKE")
        private ReactionType myReaction;

        @Schema(description = "반응별 카운트 (KEY: 반응타입, VALUE: 개수)", example = "{\"LIKE\": 10, \"LOVE\": 5}")
        private Map<ReactionType, Long> counts;
    }

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "포스트 상세 조회 응답")
    public static class PostDetailResponse {
        @Schema(description = "포스트 ID", example = "1")
        private Long id;
        @Schema(description = "비콘 ID (H3 Index)", example = "89283082807ffff")
        private String beaconId;
        @Schema(description = "위도", example = "37.5665")
        private Double latitude;
        @Schema(description = "경도", example = "126.9780")
        private Double longitude;
        @Schema(description = "장소명", example = "스타벅스 강남점")
        private String locationName;

        @Schema(description = "작성자 정보 (UserResponse 통일)")
        private UserDto.UserResponse user;

        @Schema(description = "미디어 목록")
        private List<MediaResponse> mediaList;

        @Schema(description = "공동 작업자 목록 (UserResponse 통일)")
        private List<UserDto.UserResponse> collaborators;

        @Schema(description = "생성 시간")
        private LocalDateTime createdAt;
        @Schema(description = "마지막 수정 시간")
        private LocalDateTime updatedAt;
        @Schema(description = "30일 후 자동 보관 설정 여부")
        @JsonProperty("isArchived")
        private Boolean isArchived;
        @Schema(description = "이 게시물의 총 댓글 수", example = "5")
        private Long commentCount;

        @Schema(description = "이 게시물의 총 반응(이모지) 수", example = "15")
        private Long reactionCount;

        @Schema(description = "반응(이모지) 요약 정보")
        private ReactionSummary reactions;

        @Schema(description = "게시글 현재 상태 (ACTIVE: 게시중, ARCHIVED: 보관됨)", example = "ACTIVE")
        private PostStatus status;

        public static PostDetailResponse from(Post post) {
            return PostDetailResponse.builder()
                    .id(post.getId())
                    .beaconId(post.getBeaconId())
                    .latitude(post.getLatitude())
                    .longitude(post.getLongitude())
                    .locationName(post.getLocationName())
                    .user(UserDto.UserResponse.from(post.getUser()))
                    .mediaList(post.getMediaList().stream()
                            .map(MediaResponse::from)
                            .collect(Collectors.toList()))
                    .collaborators(post.getCollaborators().stream()
                            .map(PostCollaborator::getUser)
                            .map(UserDto.UserResponse::from)
                            .collect(Collectors.toList()))
                    .createdAt(post.getCreatedAt())
                    .updatedAt(post.getUpdatedAt())
                    .isArchived(post.isArchived())
                    .commentCount(0L)
                    .reactionCount(0L)
                    .status(post.getStatus())
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
        private Long nextCursor;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "방문한 장소(Footprint) 응답")
    public static class VisitedPlaceResponse {
        @Schema(description = "비콘 ID", example = "89283082807ffff")
        private String beaconId;

        @Schema(description = "비콘 중심 위도", example = "37.5665")
        private Double latitude;

        @Schema(description = "비콘 중심 경도", example = "126.9780")
        private Double longitude;

        @Schema(description = "대표 장소명 (가장 최근 게시물 기준)", example = "서울시청")
        private String locationName;

        @Schema(description = "대표 썸네일 URL", example = "https://cdn.../image.jpg")
        private String thumbnailUrl;

        @Schema(description = "해당 장소에서의 포스트 개수", example = "3")
        private Long postCount;

        @Schema(description = "마지막 방문 일시")
        private LocalDateTime lastVisitedAt;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "이 장소에 방문했던 친구 정보")
    public static class FriendVisitResponse {
        @Schema(description = "나의 방문 여부 (true: 나도 찍은 적 있음, false: 처음)", example = "false")
        private boolean isVisitedByMe;

        @Schema(description = "방문했던 친구 목록 (최대 3~5명)")
        private List<UserDto.UserResponse> visitors;

        @Schema(description = "총 방문한 친구 수", example = "5")
        private Long totalCount;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "친구 지도 마커 상세 응답 (User + Beacon + Post 그룹화)")
    public static class FriendMapMarkerResponse {
        @Schema(description = "작성자(친구) 정보")
        private UserDto.UserResponse user;

        @Schema(description = "비콘(위치) 정보")
        private BeaconInfo beacon;

        @Schema(description = "게시글 요약 정보")
        private PostInfo post;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class BeaconInfo {
        @Schema(description = "비콘 ID", example = "89283082807ffff")
        private String id;
        @Schema(description = "위도", example = "37.5665")
        private Double latitude;
        @Schema(description = "경도", example = "126.9780")
        private Double longitude;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PostInfo {
        @Schema(description = "게시글 ID", example = "100")
        private Long id;
        @Schema(description = "썸네일 URL", example = "https://cdn.../image.jpg")
        private String thumbnailUrl;
    }
}