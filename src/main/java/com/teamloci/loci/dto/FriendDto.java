package com.teamloci.loci.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

public class FriendDto {

    @Getter
    @NoArgsConstructor
    @Schema(description = "연락처 기반 친구 매칭 요청 Body")
    public static class ContactListRequest {
        @Schema(description = "주소록에 있는 전화번호 리스트", example = "[\"010-1234-5678\", \"+82 10 1234 5678\"]")
        @NotNull
        private List<String> phoneNumbers;
    }

    @Getter
    @NoArgsConstructor
    @Schema(description = "친구 추가/삭제 요청 Body")
    public static class FriendManageByIdRequest {
        @Schema(description = "상대방 User ID", example = "2")
        @NotNull
        private Long targetUserId;
    }
}