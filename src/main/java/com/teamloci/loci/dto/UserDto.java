package com.teamloci.loci.dto;

import com.teamloci.loci.domain.User;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

public class UserDto {

    @Getter
    @NoArgsConstructor
    public static class ProfileUpdateRequest {
        @NotBlank(message = "닉네임을 입력해주세요.")
        private String nickname;
        private String bio;
    }

    @Getter
    @NoArgsConstructor
    public static class ProfileUrlUpdateRequest {
        private String profileUrl;
    }

    @Getter
    @NoArgsConstructor
    public static class FcmTokenUpdateRequest {
        @NotBlank(message = "FCM 토큰이 필요합니다.")
        private String fcmToken;
    }

    @Getter
    @AllArgsConstructor
    public static class UserResponse {
        private Long id;
        private String nickname;
        private String bio;
        private String profileUrl;
        private String email;
        private String provider;
        private String providerId;
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