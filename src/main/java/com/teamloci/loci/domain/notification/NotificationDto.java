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
                    .build();
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
}