package com.teamfiv5.fiv5.dto;

import com.teamfiv5.fiv5.domain.User;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

public class FriendDto {

    // --- 요청 DTO ---

    @Getter
    @NoArgsConstructor
    public static class FindFriendsByTokensRequest {
        @NotNull
        private List<String> tokens;
    }

    /**
     * API 4 (친구 수락) DTO
     */
    @Getter
    @NoArgsConstructor
    public static class FriendManageByIdRequest { // (DTO 이름 수정)
        @NotNull(message = "상대방의 ID가 필요합니다.")
        private Long targetUserId; // 요청을 보낸 사람(requester) 또는 수락할 사람의 ID
    }

    // --- 응답 DTO ---

    @Getter
    @AllArgsConstructor
    public static class DiscoveryTokenResponse {
        private String bluetoothToken;
    }

    // UserResponse 대신 블루투스 발견 전용 DTO 사용
    @Getter
    @AllArgsConstructor
    public static class DiscoveredUserResponse {
        private Long id;
        private String nickname;
        private String bio;
        private String profileUrl;

        private String bluetoothToken;

        private FriendshipStatusInfo friendshipStatus;

        public static DiscoveredUserResponse of(User user, FriendshipStatusInfo status) {
            return new DiscoveredUserResponse(
                    user.getId(),
                    user.getNickname(),
                    user.getBio(),
                    user.getProfileUrl(),
                    user.getBluetoothToken(), // (수정) id 대신 토큰 반환 -> (유지)
                    status
            );
        }
    }

    /**
     * NONE: 아무 관계 아님
     * FRIEND: 친구 상태
     * PENDING_ME_TO_THEM: 내가 상대에게 요청 보냄
     * PENDING_THEM_TO_ME: 상대가 나에게 요청 보냄
     */
    public enum FriendshipStatusInfo {
        NONE, FRIEND, PENDING_ME_TO_THEM, PENDING_THEM_TO_ME
    }
}