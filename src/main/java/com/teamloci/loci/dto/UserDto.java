package com.teamloci.loci.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.teamloci.loci.domain.User;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

public class UserDto {

    @Getter
    @NoArgsConstructor
    @Schema(description = "프로필 수정 요청 (변경할 필드만 값을 보내세요. 변경하지 않을 필드는 null 또는 아예 안 보내면 됩니다.)")
    public static class ProfileUpdateRequest {

        @Schema(description = "변경할 핸들(ID). 영문 소문자, 숫자, _, . 만 허용. (null이면 기존 값 유지)", example = "happy_quokka")
        @Pattern(regexp = "^[a-z0-9._]+$", message = "핸들은 영문 소문자, 숫자, 마침표(.), 밑줄(_)만 사용할 수 있습니다.")
        private String handle;

        @Schema(description = "변경할 표시 이름(닉네임). 한글 등 자유 형식. (null이면 기존 값 유지)", example = "행복한 쿼카")
        @Size(min = 1, message = "닉네임은 최소 1글자 이상이어야 합니다.")
        private String nickname;
    }

    @Getter
    @AllArgsConstructor
    @Schema(description = "핸들 중복 검사 응답")
    public static class HandleCheckResponse {
        @Schema(description = "사용 가능 여부 (true: 사용 가능, false: 중복됨)", example = "true")
        @JsonProperty("isValidHandle")
        private boolean isValidHandle;
    }

    @Getter
    @NoArgsConstructor
    @Schema(description = "프로필 이미지 URL 변경 요청")
    public static class ProfileUrlUpdateRequest {
        @Schema(description = "S3 이미지 URL (빈 문자열이나 null 전송 시 프로필 사진 삭제)", example = "https://fiv5-assets.s3.../profile.jpg")
        private String profileUrl;
    }

    @Getter
    @NoArgsConstructor
    @Schema(description = "FCM 토큰 갱신 요청")
    public static class FcmTokenUpdateRequest {
        @Schema(description = "새로운 FCM 기기 토큰", required = true, example = "fcm_token_string...")
        @NotBlank(message = "FCM 토큰이 필요합니다.")
        private String fcmToken;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @Schema(description = "사용자 정보 응답")
    public static class UserResponse {
        @Schema(description = "유저 고유 ID (DB PK)", example = "1")
        private Long id;

        @Schema(description = "사용자 핸들 (고유 ID, @handle)", example = "happy_quokka")
        private String handle;

        @Schema(description = "표시 이름 (닉네임)", example = "행복한 쿼카")
        private String nickname;

        @Schema(description = "프로필 이미지 URL", example = "https://fiv5.../profile.jpg")
        private String profileUrl;

        @Schema(description = "가입 일시")
        private LocalDateTime createdAt;

        @Schema(description = "나와의 관계 (NONE: 남, FRIEND: 친구, PENDING_SENT: 요청 보냄, PENDING_RECEIVED: 요청 받음, SELF: 나)", example = "FRIEND")
        private String relationStatus;

        public static UserResponse from(User user) {
            return new UserResponse(
                    user.getId(),
                    user.getHandle(),
                    user.getNickname(),
                    user.getProfileUrl(),
                    user.getCreatedAt(),
                    "NONE"
            );
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