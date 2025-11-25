package com.teamloci.loci.controller;

import com.teamloci.loci.dto.NotificationDto;
import com.teamloci.loci.global.exception.CustomException;
import com.teamloci.loci.global.exception.code.ErrorCode;
import com.teamloci.loci.global.response.CustomResponse;
import com.teamloci.loci.global.security.AuthenticatedUser;
import com.teamloci.loci.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
}