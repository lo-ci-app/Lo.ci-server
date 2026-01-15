package com.teamloci.loci.domain.block;

import com.teamloci.loci.global.auth.AuthenticatedUser;
import com.teamloci.loci.global.common.CustomResponse;
import com.teamloci.loci.global.error.CustomException;
import com.teamloci.loci.global.error.ErrorCode;
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

    @Operation(summary = "유저 차단/해제 토글", description = "대상 유저를 차단하거나, 이미 차단된 경우 해제합니다. (원버튼 방식)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "성공",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                                "timestamp": "2026-01-14T10:00:00.000",
                                "isSuccess": true,
                                "code": "COMMON200",
                                "message": "성공적으로 요청을 수행했습니다.",
                                "result": "차단되었습니다."
                            }
                            """)))
    })
    @PostMapping("/{targetUserId}/block")
    public ResponseEntity<CustomResponse<String>> toggleBlock(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Parameter(description = "차단/해제할 대상 유저 ID", example = "5", required = true)
            @PathVariable Long targetUserId
    ) {
        if (user == null) throw new CustomException(ErrorCode.UNAUTHORIZED);

        boolean isBlocked = userBlockService.toggleBlock(user.getUserId(), targetUserId);
        String message = isBlocked ? "차단되었습니다." : "차단이 해제되었습니다.";

        return ResponseEntity.ok(CustomResponse.ok(message));
    }
}