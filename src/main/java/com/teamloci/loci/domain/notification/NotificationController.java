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
}