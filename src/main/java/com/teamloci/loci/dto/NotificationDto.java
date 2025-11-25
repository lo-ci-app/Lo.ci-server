package com.teamloci.loci.dto;

import com.teamloci.loci.domain.Notification;
import com.teamloci.loci.domain.NotificationType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

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

        public static Response from(Notification notification) {
            return Response.builder()
                    .id(notification.getId())
                    .title(notification.getTitle())
                    .body(notification.getBody())
                    .type(notification.getType())
                    .relatedId(notification.getRelatedId())
                    .isRead(notification.isRead())
                    .createdAt(notification.getCreatedAt())
                    .build();
        }
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class ListResponse {
        private List<Response> notifications;
        private boolean hasNext;
        private Long nextCursor;
        private long unreadCount;
    }
}