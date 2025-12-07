package com.teamloci.loci.domain.intimacy.dto;

import com.teamloci.loci.domain.user.UserDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

public class IntimacyDto {

    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "친밀도 상세 정보 (유저 정보 포함)")
    public static class DetailResponse {
        @Schema(description = "상대방 유저 정보")
        private UserDto.UserResponse targetUser;

        @Schema(description = "현재 친밀도 레벨", example = "3")
        private int level;

        @Schema(description = "현재 누적 친밀도 점수", example = "250")
        private Long score;
    }
}