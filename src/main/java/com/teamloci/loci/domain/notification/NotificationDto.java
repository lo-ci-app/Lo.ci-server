package com.teamloci.loci.domain.notification;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

public class NotificationDto {

    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "알림 응답")
    public static class Response {
        private Long id;
        private String title;
        private String body;
        private NotificationType type;
        private Long relatedId;
        private boolean isRead;
        private LocalDateTime createdAt;

        @Schema(description = "알림 썸네일 이미지 URL")
        private String thumbnailUrl;

        @Schema(description = "썸네일 타입 (POST_IMAGE: 사각형, APP_LOGO: 앱 로고, USER_PROFILE: 원형)", example = "POST_IMAGE")
        private String thumbnailType;

        public static Response from(Notification notification) {
            return Response.builder()
                    .id(notification.getId())
                    .title(notification.getTitle())
                    .body(notification.getBody())
                    .type(notification.getType())
                    .relatedId(notification.getRelatedId())
                    .isRead(notification.isRead())
                    .createdAt(notification.getCreatedAt())
                    .thumbnailUrl(notification.getThumbnailUrl())
                    .thumbnailType(getThumbnailType(notification.getType()))
                    .build();
        }

        private static String getThumbnailType(NotificationType type) {
            return switch (type) {
                case NEW_POST, POST_TAGGED, POST_REACTION, COMMENT_LIKE -> "POST_IMAGE";

                case LOCI_TIME -> "APP_LOGO";

                default -> "USER_PROFILE";
            };
        }
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(name = "NotificationListResponse")
    public static class ListResponse {
        private List<Response> notifications;
        private boolean hasNext;
        private Long nextCursor;
        private long unreadCount;
    }

    @Getter
    @NoArgsConstructor
    @Schema(description = "콕 찌르기(넛지) 요청")
    public static class NudgeRequest {
        @Schema(description = "보낼 메시지 (친밀도 레벨 6 이상만 적용, 미만은 무시됨)", example = "밥 먹었어?")
        private String message;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "넛지(찌르기) 요청 응답")
    public static class NudgeResponse {

        @Schema(description = "넛지 발송 성공 여부 (true: 발송됨, false: 쿨타임 중)", example = "false")
        private boolean isSent;
        @Schema(description = "쿨타임 잔여 시간 (발송 성공 시 null)", example = "58분 30초 남음")
        private String message;
        @Schema(description = "쿨타임 잔여 초 (타이머 표시용)", example = "3510")
        private Long remainingSeconds;
    }
}