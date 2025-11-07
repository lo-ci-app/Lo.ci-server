// 경로: src/main/java/com/teamfiv5/fiv5/dto/UserDto.java
package com.teamfiv5.fiv5.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.teamfiv5.fiv5.domain.User;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class UserDto {
    /**
     * 닉네임 + bio 수정 요청 (fetchUser)
     * (PATCH /api/v1/users/me/profile)
     */
    @Getter
    @NoArgsConstructor
    public static class ProfileUpdateRequest {
        @NotBlank(message = "닉네임을 입력해주세요.")
        private String nickname;
        private String bio;
    }

    /**
     * 프로필 사진 URL 수정 요청 (fetchProfile)
     * (PATCH /api/v1/users/me/profileUrl)
     */
    @Getter
    @NoArgsConstructor
    public static class ProfileUrlUpdateRequest {
        // null 허용 (프로필 사진 삭제)
        private String profileUrl;
    }


    /**
     * "유저 전체 정보" 공통 응답 DTO
     * (프론트엔드 Swift 모델과 snake_case 키를 일치시킴)
     */
    @Getter
    @AllArgsConstructor
    public static class UserResponse {
        private Long id;
        private String nickname;
        private String bio;

        @JsonProperty("profile_url")
        private String profileUrl;

        private String email;
        private String provider;

        @JsonProperty("provider_id")
        private String providerId;

        @JsonProperty("created_at")
        private LocalDateTime createdAt;

        public static UserResponse from(User user) {
            return new UserResponse(
                    user.getId(),
                    user.getNickname(),
                    user.getBio(),
                    user.getProfileUrl(),
                    user.getEmail(),
                    user.getProvider(),
                    user.getProviderId(),
                    user.getCreatedAt()
            );
        }
    }
}