package com.teamloci.loci.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.cloud.Timestamp;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Schema(description = "채팅 관련 DTO")
public class ChatDto {

    @Getter
    @NoArgsConstructor
    @Schema(description = "API 1: 메시지 전송 요청 Body")
    public static class SendMessageRequest {

        @Schema(description = "메시지를 받을 상대방(친구)의 User ID", example = "2")
        @NotNull(message = "메시지를 보낼 상대방의 ID가 필요합니다.")
        private Long receiverId;

        @Schema(description = "전송할 메시지 본문", example = "안녕! 뭐해?")
        @NotBlank(message = "메시지 내용을 입력해주세요.")
        private String messageText;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "API 2: 채팅방 목록 조회 응답")
    public static class ChatRoomResponse {

        @Schema(description = "채팅방 고유 ID (예: 1_5)", example = "1_5")
        private String roomId;

        @Schema(description = "채팅방 참여자 ID 목록", example = "[1, 5]")
        private List<Long> participants;

        @Schema(description = "마지막으로 수신된 메시지 요약")
        private LastMessageInfo lastMessage;

        @Schema(description = "채팅방 참여자 정보 (Key: User ID)")
        private Map<String, ParticipantInfo> participantInfo;

        @Schema(description = "참여자별 안 읽은 메시지 수 (Key: User ID)", example = "{\"1\": 0, \"5\": 2}")
        private Map<String, Long> unreadCount;

        @Schema(description = "채팅방이 마지막으로 업데이트된 시간")
        private Timestamp updatedAt;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "채팅방 목록에 표시될 마지막 메시지 정보")
    public static class LastMessageInfo {
        @Schema(description = "메시지 본문", example = "응, 코딩하고 있어")
        private String text;

        @Schema(description = "메시지를 보낸 사람의 User ID", example = "5")
        private Long senderId;

        @Schema(description = "메시지 전송 시간 (Firestore Timestamp)")
        private Timestamp timestamp;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "채팅방 목록에 표시될 참여자 정보")
    public static class ParticipantInfo {
        @Schema(description = "참여자 닉네임", example = "행복한쿼카")
        private String nickname;

        @Schema(description = "참여자 프로필 이미지 URL", example = "https://fiv5.../profile.png")
        private String profileUrl;
    }
}