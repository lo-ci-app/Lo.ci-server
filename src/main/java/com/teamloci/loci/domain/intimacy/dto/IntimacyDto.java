package com.teamloci.loci.domain.intimacy.dto;

import com.teamloci.loci.domain.user.UserDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

public class IntimacyDto {

    @Getter
    @Builder
    @Schema(description = "친밀도 상세 정보")
    public static class DetailResponse {

        @Schema(description = "상대방 유저 정보")
        private UserDto.UserResponse targetUser;

        @Schema(description = "현재 친구와의 친밀도 레벨", example = "1")
        private int level;

        @Schema(description = "현재 친구와의 누적 점수 (진행도 표시용)", example = "50")
        private Long score;

        @Schema(description = "다음 레벨 달성 기준 점수 (null이면 만렙)", example = "100")
        private Long nextLevelScore;

        @Schema(description = "나의 '레벨 총합' (모든 친구와의 레벨 합계)", example = "5")
        private Integer myTotalLevel;
    }
}