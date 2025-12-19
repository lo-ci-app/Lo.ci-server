package com.teamloci.loci.domain.settings;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class SettingsDto {

    @Getter
    @NoArgsConstructor
    @Schema(description = "사용자 설정 변경 요청 (변경하려는 값만 true/false로 전송)")
    public static class UpdateRequest {

        @Schema(description = "게시물 자동 보관 여부", example = "true", nullable = true)
        private Boolean isAutoArchive;

        @Schema(description = "친구의 새 게시물 알림 수신 여부", example = "true", nullable = true)
        private Boolean isNewPostPushEnabled;

        @Schema(description = "로키 타임(Loci Time) 알림 수신 여부", example = "true", nullable = true)
        private Boolean isLociTimePushEnabled;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "사용자 설정 정보 응답")
    public static class Response {

        @Schema(description = "게시물 자동 보관 여부", example = "true")
        private boolean isAutoArchive;

        @Schema(description = "친구의 새 게시물 알림 수신 여부", example = "true")
        private boolean isNewPostPushEnabled;

        @Schema(description = "로키 타임(Loci Time) 알림 수신 여부", example = "true")
        private boolean isLociTimePushEnabled;
    }
}