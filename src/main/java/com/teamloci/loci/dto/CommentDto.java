package com.teamloci.loci.dto;

import com.teamloci.loci.domain.PostComment;
import com.teamloci.loci.domain.User;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

public class CommentDto {

    @Getter
    @NoArgsConstructor
    @Schema(description = "댓글 작성 요청")
    public static class CreateRequest {
        @NotBlank
        @Size(max = 500, message = "댓글은 500자 이내로 작성해주세요.")
        @Schema(description = "댓글 내용", example = "여기 진짜 예쁘다!")
        private String content;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "댓글 응답")
    public static class Response {
        @Schema(description = "댓글 ID", example = "10")
        private Long id;

        @Schema(description = "댓글 내용")
        private String content;

        @Schema(description = "작성자 정보 (관계 포함)")
        private AuthorInfo author;

        @Schema(description = "작성 시간")
        private LocalDateTime createdAt;

        public static Response of(PostComment comment, String relationStatus) {
            return Response.builder()
                    .id(comment.getId())
                    .content(comment.getContent())
                    .author(AuthorInfo.of(comment.getUser(), relationStatus))
                    .createdAt(comment.getCreatedAt())
                    .build();
        }
    }

    @Getter
    @AllArgsConstructor
    @Builder
    @Schema(description = "댓글 작성자 정보")
    public static class AuthorInfo {
        @Schema(description = "유저 ID")
        private Long id;

        @Schema(description = "닉네임")
        private String nickname;

        @Schema(description = "프로필 이미지 URL")
        private String profileUrl;

        @Schema(description = "나와의 관계 (SELF, FRIEND, NONE, PENDING_SENT, PENDING_RECEIVED)", example = "FRIEND")
        private String relationStatus;

        public static AuthorInfo of(User user, String relationStatus) {
            return AuthorInfo.builder()
                    .id(user.getId())
                    .nickname(user.getNickname())
                    .profileUrl(user.getProfileUrl())
                    .relationStatus(relationStatus)
                    .build();
        }
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "댓글 목록 (커서 기반)")
    public static class ListResponse {
        @Schema(description = "댓글 데이터 목록")
        private List<Response> comments;

        @Schema(description = "다음 페이지 존재 여부")
        private boolean hasNext;

        @Schema(description = "다음 요청에 사용할 커서 ID")
        private Long nextCursor;

        @Schema(description = "이 게시물의 전체 댓글 수", example = "42")
        private Long totalCount;
    }
}