package com.teamloci.loci.service;

import com.google.api.core.ApiFuture;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.*;
import com.teamloci.loci.domain.User;
import com.teamloci.loci.dto.ChatDto;
import com.teamloci.loci.global.exception.CustomException;
import com.teamloci.loci.global.exception.code.ErrorCode;
import com.teamloci.loci.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final Firestore firestore;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    @Transactional
    public void sendMessage(Long senderId, Long receiverId, String messageText) {

        User sender = findUserById(senderId);
        User receiver = findUserById(receiverId);

        String roomId = Math.min(senderId, receiverId) + "_" + Math.max(senderId, receiverId);

        Map<String, Object> messageData = createMessageData(senderId, messageText);

        addMessageToFirestore(roomId, messageData);

        updateRoomSummary(roomId, sender, receiver, messageData);

        sendFcmForMessage(receiver, sender.getNickname(), messageText);
    }

    private Map<String, Object> createMessageData(Long senderId, String messageText) {
        Map<String, Object> messageData = new HashMap<>();
        messageData.put("senderId", senderId);
        messageData.put("text", messageText);
        messageData.put("timestamp", FieldValue.serverTimestamp());
        return messageData;
    }

    private void addMessageToFirestore(String roomId, Map<String, Object> messageData) {
        firestore.collection("chat_rooms").document(roomId)
                .collection("messages").add(messageData);
    }

    private void updateRoomSummary(String roomId, User sender, User receiver, Map<String, Object> messageData) {
        DocumentReference roomRef = firestore.collection("chat_rooms").document(roomId);

        Map<String, Object> roomUpdate = new HashMap<>();
        roomUpdate.put("lastMessage", messageData);
        roomUpdate.put("updatedAt", FieldValue.serverTimestamp());
        roomUpdate.put("unreadCount." + String.valueOf(receiver.getId()), FieldValue.increment(1));

        roomUpdate.put("participants", List.of(sender.getId(), receiver.getId()));
        roomUpdate.put("participantInfo." + sender.getId(),
                Map.of("nickname", sender.getNickname(), "profileUrl", sender.getProfileUrl() != null ? sender.getProfileUrl() : "")
        );
        roomUpdate.put("participantInfo." + receiver.getId(),
                Map.of("nickname", receiver.getNickname(), "profileUrl", receiver.getProfileUrl() != null ? receiver.getProfileUrl() : "")
        );

        roomRef.set(roomUpdate, SetOptions.merge());
    }

    private void sendFcmForMessage(User receiver, String senderNickname, String messageText) {
        String receiverFcmToken = receiver.getFcmToken();
        if (StringUtils.hasText(receiverFcmToken)) {
            notificationService.sendDirectMessageNotification(
                    receiverFcmToken,
                    senderNickname,
                    messageText
            );
        }
    }

    public List<ChatDto.ChatRoomResponse> getChatRooms(Long myUserId) {

        ApiFuture<QuerySnapshot> future = firestore.collection("chat_rooms")
                .whereArrayContains("participants", myUserId)
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .get();

        try {
            List<QueryDocumentSnapshot> documents = future.get().getDocuments();

            return documents.stream()
                    .map(this::mapDocumentToChatRoomResponse)
                    .collect(Collectors.toList());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Firestore 조회(getChatRooms) 중 인터럽트 발생: {}", e.getMessage());
            throw new CustomException(ErrorCode.CHAT_ROOM_LIST_FAILED);
        } catch (Exception e) {
            log.error("Firestore 조회(getChatRooms) 실패 (색인 문제일 수 있음): {}", e.getMessage());
            throw new CustomException(ErrorCode.CHAT_ROOM_LIST_FAILED);
        }
    }

    private ChatDto.ChatRoomResponse mapDocumentToChatRoomResponse(QueryDocumentSnapshot doc) {

        ChatDto.LastMessageInfo lastMessageInfo = null;
        if (doc.get("lastMessage") instanceof Map) {
            Map<String, Object> lastMessageMap = (Map<String, Object>) doc.get("lastMessage");
            lastMessageInfo = ChatDto.LastMessageInfo.builder()
                    .text(lastMessageMap.get("text") instanceof String ? (String) lastMessageMap.get("text") : null)
                    .senderId(lastMessageMap.get("senderId") instanceof Number ? ((Number) lastMessageMap.get("senderId")).longValue() : null)
                    .timestamp(lastMessageMap.get("timestamp") instanceof Timestamp ? (Timestamp) lastMessageMap.get("timestamp") : null)
                    .build();
        }

        Map<String, ChatDto.ParticipantInfo> participantInfo = new HashMap<>();
        if (doc.get("participantInfo") instanceof Map) {
            Map<String, Object> participantInfoMap = (Map<String, Object>) doc.get("participantInfo");
            for (Map.Entry<String, Object> entry : participantInfoMap.entrySet()) {
                if (entry.getValue() instanceof Map) {
                    Map<String, Object> info = (Map<String, Object>) entry.getValue();
                    participantInfo.put(entry.getKey(), ChatDto.ParticipantInfo.builder()
                            .nickname(info.get("nickname") instanceof String ? (String) info.get("nickname") : null)
                            .profileUrl(info.get("profileUrl") instanceof String ? (String) info.get("profileUrl") : null)
                            .build());
                }
            }
        }

        Map<String, Long> unreadCount = new HashMap<>();
        if (doc.get("unreadCount") instanceof Map) {
            Map<String, Object> unreadCountMap = (Map<String, Object>) doc.get("unreadCount");
            unreadCountMap.forEach((key, value) -> {
                if (value instanceof Number) {
                    unreadCount.put(key, ((Number) value).longValue());
                }
            });
        }

        List<Long> participants = new ArrayList<>();
        if (doc.get("participants") instanceof List) {
            for (Object p : (List<?>) doc.get("participants")) {
                if (p instanceof Number) {
                    participants.add(((Number) p).longValue());
                }
            }
        }

        return ChatDto.ChatRoomResponse.builder()
                .roomId(doc.getId())
                .participants(participants)
                .lastMessage(lastMessageInfo)
                .participantInfo(participantInfo)
                .unreadCount(unreadCount)
                .updatedAt(doc.getTimestamp("updatedAt"))
                .build();
    }
}