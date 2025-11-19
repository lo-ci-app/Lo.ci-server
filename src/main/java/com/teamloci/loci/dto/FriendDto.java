package com.teamloci.loci.dto;

import com.teamloci.loci.domain.User;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

public class FriendDto {

    @Getter
    @NoArgsConstructor
    public static class FindFriendsByTokensRequest {
        @NotNull
        private List<String> tokens;
    }

    @Getter
    @NoArgsConstructor
    public static class FriendManageByIdRequest { // (DTO 이름 수정)
        @NotNull(message = "상대방의 ID가 필요합니다.")
        private Long targetUserId; // 요청을 보낸 사람(requester) 또는 수락할 사람의 ID
    }

    @Getter
    @AllArgsConstructor
    public static class DiscoveryTokenResponse {
        private String bluetoothToken;
    }

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
                    user.getBluetoothToken(),
                    status
            );
        }
    }

    public enum FriendshipStatusInfo {
        NONE, FRIEND, PENDING_ME_TO_THEM, PENDING_THEM_TO_ME
    }
}