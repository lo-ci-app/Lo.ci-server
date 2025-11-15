package com.teamfiv5.fiv5.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.ApnsConfig;
import com.google.firebase.messaging.Aps;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class NotificationService {

    public void sendFriendRequestNotification(String targetFcmToken, String requesterNickname) {
        try {
            String title = "새로운 친구 요청";
            String body = requesterNickname + "님이 친구 요청을 보냈어요!";

            Message message = Message.builder()
                    .setToken(targetFcmToken)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .setApnsConfig(ApnsConfig.builder()
                            .setAps(Aps.builder()
                                    // (5) .setAlert() 빌더는 여기서 사용 X
                                    .setSound("default")
                                    .setContentAvailable(true)
                                    .build())
                            .build())

                    .putData("type", "FRIEND_REQUEST")
                    .putData("requesterNickname", requesterNickname)
                    .build();

            String response = FirebaseMessaging.getInstance().send(message);
            log.info("FCM 발송 성공: " + response);

        } catch (FirebaseMessagingException e) {
            log.error("FCM 발송 실패: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("FCM 메시지 빌드 중 알 수 없는 오류", e);
        }
    }
}