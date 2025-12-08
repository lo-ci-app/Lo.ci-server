package com.teamloci.loci.domain.user;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.teamloci.loci.domain.intimacy.entity.FriendshipIntimacy;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

public class UserDto {

    @Getter
    @NoArgsConstructor
    @Schema(description = "프로필 수정 요청")
    public static class ProfileUpdateRequest {

        @Schema(description = "변경할 핸들(ID)", example = "happy_quokka")
        @Pattern(regexp = "^[a-z0-9._]+$", message = "핸들은 영문 소문자, 숫자, 마침표(.), 밑줄(_)만 사용할 수 있습니다.")
        private String handle;

        @Schema(description = "변경할 표시 이름(닉네임)", example = "행복한 쿼카")
        @Size(min = 1, message = "닉네임은 최소 1글자 이상이어야 합니다.")
        private String nickname;

        @Schema(description = "자동 보관 설정 변경", example = "true")
        private Boolean isAutoArchive;
    }

    @Getter
    @AllArgsConstructor
    @Schema(description = "핸들 중복 검사 응답")
    public static class HandleCheckResponse {
        @Schema(description = "사용 가능 여부", example = "true")
        @JsonProperty("isValidHandle")
        private boolean isValidHandle;
    }

    @Getter
    @NoArgsConstructor
    @Schema(description = "프로필 이미지 URL 변경 요청")
    public static class ProfileUrlUpdateRequest {
        @Schema(description = "S3 이미지 URL", example = "https://fiv5.../profile.jpg")
        private String profileUrl;
    }

    @Getter
    @NoArgsConstructor
    @Schema(description = "FCM 토큰 갱신 요청")
    public static class FcmTokenUpdateRequest {
        @Schema(description = "새로운 FCM 기기 토큰", required = true)
        private String fcmToken;
    }

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @Schema(description = "사용자 정보 응답")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class UserResponse {
        @Schema(description = "유저 고유 ID", example = "1")
        private Long id;

        @Schema(description = "사용자 핸들", example = "happy_quokka")
        private String handle;

        @Schema(description = "닉네임", example = "행복한 쿼카")
        private String nickname;

        @Schema(description = "프로필 이미지 URL", example = "https://fiv5.../profile.jpg")
        private String profileUrl;

        @Schema(description = "가입 일시")
        private LocalDateTime createdAt;

        @Schema(description = "나와의 관계", example = "FRIEND")
        private String relationStatus;

        @Schema(description = "친구 수", example = "12")
        private Long friendCount;

        @Schema(description = "게시물 수", example = "5")
        private Long postCount;

        @Schema(description = "연속 업로드 일수 (Streak) 🔥", example = "3")
        private Long streakCount;

        @Schema(description = "방문한 장소 수 (Flags) 🚩", example = "7")
        private Long visitedPlaceCount;

        @Schema(description = "친밀도 레벨 (친구 목록 조회 시 포함)", example = "3")
        private Integer intimacyLevel;

        @Schema(description = "친밀도 점수 (친구 목록 조회 시 포함)", example = "250")
        private Long intimacyScore;

        @Schema(description = "해당 유저의 총 친밀도 레벨 합", example = "15")
        private Integer totalIntimacyLevel;

        @Schema(description = "자동 보관 설정 여부")
        private boolean isAutoArchive;

        public static UserResponse of(User user, String relationStatus, long friendCount, long postCount) {
            return of(user, relationStatus, friendCount, postCount, 0L, 0L);
        }

        public static UserResponse of(User user, String relationStatus, long friendCount, long postCount, long streakCount, long visitedPlaceCount) {
            return UserResponse.builder()
                    .id(user.getId())
                    .handle(user.getHandle())
                    .nickname(user.getNickname())
                    .profileUrl(user.getProfileUrl())
                    .createdAt(user.getCreatedAt())
                    .relationStatus(relationStatus)
                    .friendCount(friendCount)
                    .postCount(postCount)
                    .streakCount(streakCount)
                    .visitedPlaceCount(visitedPlaceCount)
                    .isAutoArchive(user.isAutoArchive())
                    .build();
        }

        public static UserResponse from(User user) {
            return UserResponse.builder()
                    .id(user.getId())
                    .handle(user.getHandle())
                    .nickname(user.getNickname())
                    .profileUrl(user.getProfileUrl())
                    .createdAt(user.getCreatedAt())
                    .relationStatus("NONE")
                    .friendCount(0L)
                    .postCount(0L)
                    .streakCount(0L)
                    .visitedPlaceCount(0L)
                    .isAutoArchive(user.isAutoArchive())
                    .build();
        }

        public void applyIntimacyInfo(FriendshipIntimacy intimacy, int totalLevel) {
            if ("FRIEND".equals(this.relationStatus)) {
                this.totalIntimacyLevel = totalLevel;
                if (intimacy != null) {
                    this.intimacyLevel = intimacy.getLevel();
                    this.intimacyScore = intimacy.getTotalScore();
                } else {
                    this.intimacyLevel = 1;
                    this.intimacyScore = 0L;
                }
            } else if ("SELF".equals(this.relationStatus)) {
                this.totalIntimacyLevel = totalLevel;
            }
        }
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UserSearchResponse {
        private List<UserResponse> users;
        private boolean hasNext;
        private Long nextCursor;
    }
}