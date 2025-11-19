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
        @Schema(description = "포스트 본문 (비어있을 수 있음)", example = "오늘의 기록")
        private String contents;

        @Schema(description = "미디어 목록 (없으면 빈 배열 또는 null)")
        private List<MediaRequest> mediaList;

        @Schema(description = "공동 작업자 User ID 목록 (없으면 빈 배열 또는 null)", example = "[2, 3]")
        private List<Long> collaboratorIds;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "포스트 상세 조회 응답")
    public static class PostDetailResponse {
        @Schema(description = "포스트 ID", example = "1")
        private Long id;
        @Schema(description = "포스트 본문", example = "오늘의 기록")
        private String contents;
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

        public static PostDetailResponse from(Post post) {
            return PostDetailResponse.builder()
                    .id(post.getId())
                    .contents(post.getContents())
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
                    .build();
        }
    }
}