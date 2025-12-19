package com.teamloci.loci.domain.settings;

import com.teamloci.loci.global.auth.AuthenticatedUser;
import com.teamloci.loci.global.common.CustomResponse;
import com.teamloci.loci.global.error.CustomException;
import com.teamloci.loci.global.error.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Settings", description = "사용자 설정 API")
@RestController
@RequestMapping("/api/v1/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final SettingsService settingsService;

    private Long getUserId(AuthenticatedUser user) {
        if (user == null) throw new CustomException(ErrorCode.UNAUTHORIZED);
        return user.getUserId();
    }

    @Operation(summary = "설정 조회",
            description = """
                    현재 사용자의 모든 설정(자동 보관, 알림 등)을 조회합니다.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "code": "COMMON200",
                              "message": "성공적으로 요청을 수행했습니다.",
                              "result": {
                                "isAutoArchive": true,
                                "isNewPostPushEnabled": true,
                                "isLociTimePushEnabled": false
                              }
                            }
                            """)))
    })
    @GetMapping
    public ResponseEntity<CustomResponse<SettingsDto.Response>> getSettings(
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ResponseEntity.ok(CustomResponse.ok(
                settingsService.getSettings(getUserId(user))
        ));
    }

    @Operation(summary = "설정 수정",
            description = """
                    사용자의 설정을 변경합니다. (Null인 필드는 기존 값을 유지)
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "code": "COMMON200",
                              "message": "성공적으로 요청을 수행했습니다.",
                              "result": {
                                "isAutoArchive": false,
                                "isNewPostPushEnabled": true,
                                "isLociTimePushEnabled": true
                              }
                            }
                            """)))
    })
    @PatchMapping
    public ResponseEntity<CustomResponse<SettingsDto.Response>> updateSettings(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestBody SettingsDto.UpdateRequest request
    ) {
        return ResponseEntity.ok(CustomResponse.ok(
                settingsService.updateSettings(getUserId(user), request)
        ));
    }
}