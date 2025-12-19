package com.teamloci.loci.domain.notification;

import com.teamloci.loci.global.error.CustomException;
import com.teamloci.loci.global.error.ErrorCode;
import com.teamloci.loci.global.common.CustomResponse;
import com.teamloci.loci.global.auth.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Notification", description = "알림 센터 API")
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    private Long getUserId(AuthenticatedUser user) {
        if (user == null) throw new CustomException(ErrorCode.UNAUTHORIZED);
        return user.getUserId();
    }

    @Operation(summary = "내 알림 목록 조회", description = "받은 알림 목록을 최신순으로 조회합니다.")
    @GetMapping
    public ResponseEntity<CustomResponse<NotificationDto.ListResponse>> getMyNotifications(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Parameter(description = "이전 페이지의 마지막 알림 ID") @RequestParam(required = false) Long cursorId,
            @Parameter(description = "개수") @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(CustomResponse.ok(
                notificationService.getMyNotifications(getUserId(user), cursorId, size)
        ));
    }

    @Operation(summary = "알림 읽음 처리", description = "특정 알림을 읽음 상태로 변경합니다.")
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<CustomResponse<Void>> readNotification(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long notificationId
    ) {
        notificationService.readNotification(getUserId(user), notificationId);
        return ResponseEntity.ok(CustomResponse.ok(null));
    }

    @Operation(summary = "알림 전체 읽음 처리", description = "나에게 온 모든 알림(안 읽은 알림)을 읽음 상태로 변경하고, **처리된 알림의 개수**를 반환합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "처리 성공",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "timestamp": "2025-12-12T09:27:43.726Z",
                              "isSuccess": true,
                              "code": "COMMON200",
                              "message": "성공적으로 요청을 수행했습니다.",
                              "result": 5
                            }
                            """)))
    })
    @PatchMapping("/read-all")
    public ResponseEntity<CustomResponse<Integer>> readAllNotifications(
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        int count = notificationService.readAllNotifications(getUserId(user));
        return ResponseEntity.ok(CustomResponse.ok(count));
    }

    @Operation(
            summary = "친구 콕 찌르기 (Nudge)",
            description = """
                친밀도 레벨 3 이상인 친구에게 '콕 찌르기' 알림을 보냅니다. (쿨타임 1시간)
                
                * **성공 (`isSent`: true)**: 즉시 알림 전송
                * **실패/쿨타임 중 (`isSent`: false)**: `message`와 `remainingSeconds`에 남은 시간 정보를 반환합니다.
                """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "요청 성공 (쿨타임 안내 포함)",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "timestamp": "2025-12-16T10:14:55.150Z",
                              "isSuccess": true,
                              "code": "COMMON200",
                              "message": "성공적으로 요청을 수행했습니다.",
                              "result": {
                                "isSent": false,
                                "message": "58분 30초 뒤에 다시 찌를 수 있어요!",
                                "remainingSeconds": 3510
                              }
                            }
                            """)))
    })
    @PostMapping("/nudge/friend/{targetUserId}")
    public ResponseEntity<CustomResponse<NotificationDto.NudgeResponse>> sendNudge(
            @AuthenticationPrincipal AuthenticatedUser user,

            @Parameter(description = "콕 찌를 상대방의 유저 ID", example = "123", required = true)
            @PathVariable Long targetUserId,

            @RequestBody @Valid NotificationDto.NudgeRequest request
    ) {
        return ResponseEntity.ok(CustomResponse.ok(
                notificationService.sendNudge(user.getUserId(), targetUserId, request)
        ));
    }
}