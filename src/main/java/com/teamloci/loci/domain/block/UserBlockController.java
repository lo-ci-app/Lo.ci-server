package com.teamloci.loci.domain.block;

import com.teamloci.loci.global.auth.AuthenticatedUser;
import com.teamloci.loci.global.common.CustomResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Block", description = "유저 차단 API")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserBlockController {

    private final UserBlockService userBlockService;

    @Operation(summary = "유저 차단/해제 토글", description = "상대방을 차단하거나, 이미 차단된 경우 해제합니다. (버튼 하나로 동작)")
    @PostMapping("/{targetUserId}/block")
    public ResponseEntity<CustomResponse<String>> toggleBlock(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long targetUserId
    ) {
        boolean isBlocked = userBlockService.toggleBlock(user.getUserId(), targetUserId);
        String message = isBlocked ? "차단되었습니다." : "차단이 해제되었습니다.";

        return ResponseEntity.ok(CustomResponse.ok(message));
    }
}