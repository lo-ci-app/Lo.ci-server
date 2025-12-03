package com.teamloci.loci.domain.post.dto;

import com.teamloci.loci.domain.post.entity.PostReaction;
import com.teamloci.loci.domain.post.entity.ReactionType;
import com.teamloci.loci.domain.user.UserDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

public class ReactionDto {

    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "리액션 상세 정보 (유저 포함)")
    public static class Response {
        @Schema(description = "리액션 ID")
        private Long id;

        @Schema(description = "반응 타입 (이모지 종류)", example = "LIKE")
        private ReactionType reactionType;

        @Schema(description = "반응 남긴 유저 정보 (관계 및 통계 포함)")
        private UserDto.UserResponse user;

        @Schema(description = "반응 남긴 시간")
        private LocalDateTime createdAt;

        public static Response of(PostReaction reaction, UserDto.UserResponse userResponse) {
            return Response.builder()
                    .id(reaction.getId())
                    .reactionType(reaction.getType())
                    .user(userResponse)
                    .createdAt(reaction.getCreatedAt())
                    .build();
        }
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "리액션 목록 응답 (무한 스크롤)")
    public static class ListResponse {
        private List<Response> reactions;
        private boolean hasNext;
        private Long nextCursor;
    }
}