package com.teamloci.loci.domain.notification;

import com.google.firebase.messaging.*;
import com.teamloci.loci.domain.user.User;
import com.teamloci.loci.domain.user.UserRepository;
import com.teamloci.loci.global.error.CustomException;
import com.teamloci.loci.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
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
    private final UserRepository userRepository;

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
    public void send(User receiver, NotificationType type, String title, String body, Long relatedId, String thumbnailUrl) {
        notificationRepository.save(Notification.builder()
                .receiver(receiver)
                .type(type)
                .title(title)
                .body(body)
                .relatedId(relatedId)
                .thumbnailUrl(thumbnailUrl)
                .build());

        String token = receiver.getFcmToken();
        if (token != null && !token.isBlank()) {
            sendFcm(token, title, body, type, relatedId, thumbnailUrl);
        }
    }

    @Async("taskExecutor")
    @Transactional
    public void sendMulticast(List<Long> receiverIds, NotificationType type, String title, String body, Long relatedId, String thumbnailUrl) {
        if (receiverIds == null || receiverIds.isEmpty()) return;

        List<User> receivers = userRepository.findAllById(receiverIds);

        if (receivers.isEmpty()) return;

        List<Notification> entities = receivers.stream()
                .map(r -> Notification.builder()
                        .receiver(r)
                        .type(type)
                        .title(title)
                        .body(body)
                        .relatedId(relatedId)
                        .thumbnailUrl(thumbnailUrl)
                        .build())
                .collect(Collectors.toList());
        notificationRepository.saveAll(entities);

        List<String> tokens = receivers.stream()
                .map(User::getFcmToken)
                .filter(t -> t != null && !t.isBlank())
                .collect(Collectors.toList());

        if (!tokens.isEmpty()) {
            sendFcmMulticast(tokens, title, body, type, relatedId, thumbnailUrl);
        }
    }

    private void sendFcm(String token, String title, String body, NotificationType type, Long relatedId, String thumbnailUrl) {
        try {
            com.google.firebase.messaging.Notification.Builder notiBuilder =
                    com.google.firebase.messaging.Notification.builder()
                            .setTitle(title)
                            .setBody(body);

            if (thumbnailUrl != null && !thumbnailUrl.isBlank()) {
                notiBuilder.setImage(thumbnailUrl);
            }

            Message message = Message.builder()
                    .setToken(token)
                    .setNotification(notiBuilder.build())
                    .setApnsConfig(ApnsConfig.builder()
                            .setAps(Aps.builder().setSound("default").setContentAvailable(true).build())
                            .build())
                    .putData("type", type.name())
                    .putData("relatedId", relatedId != null ? String.valueOf(relatedId) : "")
                    .putData("thumbnailUrl", thumbnailUrl != null ? thumbnailUrl : "")
                    .build();

            FirebaseMessaging.getInstance().send(message);
        } catch (Exception e) {
            log.error(">>> [FCM Single Send Error] Exception: {}", e.getMessage());
        }
    }

    private void sendFcmMulticast(List<String> tokens, String title, String body, NotificationType type, Long relatedId, String thumbnailUrl) {
        try {
            com.google.firebase.messaging.Notification.Builder notiBuilder =
                    com.google.firebase.messaging.Notification.builder()
                            .setTitle(title)
                            .setBody(body);

            if (thumbnailUrl != null && !thumbnailUrl.isBlank()) {
                notiBuilder.setImage(thumbnailUrl);
            }

            MulticastMessage message = MulticastMessage.builder()
                    .addAllTokens(tokens)
                    .setNotification(notiBuilder.build())
                    .setApnsConfig(ApnsConfig.builder()
                            .setAps(Aps.builder().setSound("default").setContentAvailable(true).build())
                            .build())
                    .putData("type", type.name())
                    .putData("relatedId", relatedId != null ? String.valueOf(relatedId) : "")
                    .putData("thumbnailUrl", thumbnailUrl != null ? thumbnailUrl : "")
                    .build();

            BatchResponse response = FirebaseMessaging.getInstance().sendEachForMulticast(message);

            if (response.getFailureCount() > 0) {
                log.warn(">>> [FCM Multicast] Success: {}, Failure: {}", response.getSuccessCount(), response.getFailureCount());
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
            log.error(">>> [DM FCM Send Error] Exception: ", e);
        }
    }

    @Transactional
    public int readAllNotifications(Long userId) {
        int updatedCount = notificationRepository.markAllAsRead(userId);

        if (updatedCount > 0) {
            log.info("사용자 {}의 알림 {}개를 일괄 읽음 처리했습니다.", userId, updatedCount);
        }

        return updatedCount;
    }
}