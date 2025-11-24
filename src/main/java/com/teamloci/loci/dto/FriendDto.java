package com.teamloci.loci.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

public class FriendDto {

    @Getter
    @NoArgsConstructor
    @Schema(description = "연락처 기반 친구 매칭 요청 Body")
    public static class ContactListRequest {

        @Schema(description = "주소록 연락처 리스트")
        @NotNull
        @Valid
        private List<ContactRequest> contacts;
    }

    @Getter
    @NoArgsConstructor
    @Schema(description = "개별 연락처 정보")
    public static class ContactRequest {
        @Schema(description = "주소록에 저장된 이름 (필수)", example = "홍길동")
        @NotBlank(message = "이름은 필수입니다.") // DB not null 조건 맞춤
        private String name;

        @Schema(description = "전화번호 (필수)", example = "010-1234-5678")
        @NotBlank(message = "전화번호는 필수입니다.")
        private String phoneNumber;
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