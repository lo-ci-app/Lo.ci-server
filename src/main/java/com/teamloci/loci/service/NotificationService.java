package com.teamloci.loci.service;

import com.google.firebase.FirebaseApp; 
import com.google.firebase.messaging.*;
import com.teamloci.loci.domain.Notification;
import com.teamloci.loci.domain.NotificationType;
import com.teamloci.loci.domain.User;
import com.teamloci.loci.dto.NotificationDto;
import com.teamloci.loci.global.exception.CustomException;
import com.teamloci.loci.global.exception.code.ErrorCode;
import com.teamloci.loci.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationDto.ListResponse getMyNotifications(Long userId, Long cursorId, int size) {
        PageRequest pageable = PageRequest.of(0, size + 1);
        List<Notification> notifications = notificationRepository.findByUserIdWithCursor(userId, cursorId, pageable);

        boolean hasNext = false;
        if (notifications.size() > size) {
            hasNext = true;
            notifications.remove(size);
        }
        Long nextCursor = notifications.isEmpty() ? null : notifications.get(notifications.size() - 1).getId();

        List<NotificationDto.Response> dtos = notifications.stream()
                .map(NotificationDto.Response::from)
                .collect(Collectors.toList());

        long unreadCount = notificationRepository.countByReceiverIdAndIsReadFalse(userId);

        return NotificationDto.ListResponse.builder()
                .notifications(dtos)
                .hasNext(hasNext)
                .nextCursor(nextCursor)
                .unreadCount(unreadCount)
                .build();
    }

    @Transactional
    public void readNotification(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOTIFICATION_NOT_FOUND));

        if (!notification.getReceiver().getId().equals(userId)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
        notification.markAsRead();
    }

    @Transactional
    public void send(User receiver, NotificationType type, String title, String body, Long relatedId) {
        notificationRepository.save(Notification.builder()
                .receiver(receiver)
                .type(type)
                .title(title)
                .body(body)
                .relatedId(relatedId)
                .build());

        String token = receiver.getFcmToken();
        if (token != null && !token.isBlank()) {
            sendFcm(token, title, body, type, relatedId);
        }
    }

    @Transactional
    public void sendMulticast(List<User> receivers, NotificationType type, String title, String body, Long relatedId) {
        if (receivers == null || receivers.isEmpty()) return;

        List<Notification> entities = receivers.stream()
                .map(r -> Notification.builder()
                        .receiver(r)
                        .type(type)
                        .title(title)
                        .body(body)
                        .relatedId(relatedId)
                        .build())
                .collect(Collectors.toList());
        notificationRepository.saveAll(entities);

        List<String> tokens = receivers.stream()
                .map(User::getFcmToken)
                .filter(t -> t != null && !t.isBlank())
                .collect(Collectors.toList());

        if (!tokens.isEmpty()) {
            sendFcmMulticast(tokens, title, body, type, relatedId);
        }
    }

    private void sendFcm(String token, String title, String body, NotificationType type, Long relatedId) {
        try {
            Message message = Message.builder()
                    .setToken(token)
                    .setNotification(com.google.firebase.messaging.Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .setApnsConfig(ApnsConfig.builder()
                            .setAps(Aps.builder().setSound("default").setContentAvailable(true).build())
                            .build())
                    .putData("type", type.name())
                    .putData("relatedId", relatedId != null ? String.valueOf(relatedId) : "")
                    .build();

            FirebaseMessaging.getInstance().send(message);
        } catch (Exception e) {
            log.error(">>> [FCM Single Send Error] <<<");
            log.error("Active Project ID: {}", FirebaseApp.getInstance().getOptions().getProjectId());
            log.error("Service Account: {}", FirebaseApp.getInstance().getOptions().getServiceAccountId());
            log.error("Target Token: {}", token);
            log.error("Exception Message: {}", e.getMessage());
            log.error("Full StackTrace: ", e);
        }
    }

    private void sendFcmMulticast(List<String> tokens, String title, String body, NotificationType type, Long relatedId) {
        try {
            MulticastMessage message = MulticastMessage.builder()
                    .addAllTokens(tokens)
                    .setNotification(com.google.firebase.messaging.Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .setApnsConfig(ApnsConfig.builder()
                            .setAps(Aps.builder().setSound("default").setContentAvailable(true).build())
                            .build())
                    .putData("type", type.name())
                    .putData("relatedId", relatedId != null ? String.valueOf(relatedId) : "")
                    .build();

            BatchResponse response = FirebaseMessaging.getInstance().sendEachForMulticast(message);

            if (response.getFailureCount() > 0) {
                log.warn(">>> [FCM Multicast Partial Failure] <<<");
                log.warn("Success: {}, Failure: {}", response.getSuccessCount(), response.getFailureCount());
                log.warn("Active Project ID: {}", FirebaseApp.getInstance().getOptions().getProjectId());

                List<SendResponse> responses = response.getResponses();
                for (int i = 0; i < responses.size(); i++) {
                    SendResponse r = responses.get(i);
                    if (!r.isSuccessful()) {
                        log.error("--- Failure Detail (Index: {}) ---", i);
                        log.error("Target Token: {}", tokens.get(i));
                        log.error("Reason: {}", r.getException().getMessage());
                    }
                }
            }

        } catch (Exception e) {
            log.error(">>> [FCM Multicast Exception] <<<", e);
        }
    }

    public void sendDirectMessageNotification(String targetFcmToken, String senderNickname, String messageText) {
        try {
            Message message = Message.builder()
                    .setToken(targetFcmToken)
                    .setNotification(com.google.firebase.messaging.Notification.builder()
                            .setTitle(senderNickname)
                            .setBody(messageText)
                            .build())
                    .setApnsConfig(ApnsConfig.builder().setAps(Aps.builder().setSound("default").build()).build())
                    .putData("type", "DIRECT_MESSAGE")
                    .build();
            FirebaseMessaging.getInstance().send(message);
        } catch (Exception e) {
            log.error(">>> [DM FCM Send Error] <<<");
            log.error("Active Project ID: {}", FirebaseApp.getInstance().getOptions().getProjectId());
            log.error("Target Token: {}", targetFcmToken);
            log.error("Exception: ", e);
        }
    }
}