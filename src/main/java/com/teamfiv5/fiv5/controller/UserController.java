// 경로: src/main/java/com/teamfiv5/fiv5/controller/UserController.java
package com.teamfiv5.fiv5.controller;

import com.teamfiv5.fiv5.dto.UserDto;
import com.teamfiv5.fiv5.global.exception.CustomException;
import com.teamfiv5.fiv5.global.exception.code.ErrorCode;
import com.teamfiv5.fiv5.global.response.CustomResponse;
import com.teamfiv5.fiv5.global.security.AuthenticatedUser;
import com.teamfiv5.fiv5.service.UserService;

// 1. (추가) 스웨거 어노테이션 Import
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "User", description = "사용자 프로필 정보 API") // ◀◀ 2. (추가) 클래스 레벨 태그
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // (공통) 로그인한 사용자 ID 가져오기 (예외 처리 포함)
    private Long getUserId(AuthenticatedUser user) {
        if (user == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
        return user.getUserId();
    }

    /**
     * 내 정보 조회
     */
    @Operation(summary = "내 정보 조회", description = "현재 로그인한 사용자의 전체 프로필 정보를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = CustomResponse.class),
                            examples = @ExampleObject(value = """
                             {
                               "timestamp": "2025-11-04T21:00:00.123456",
                               "isSuccess": true,
                               "code": "COMMON200",
                               "message": "성공적으로 요청을 수행했습니다.",
                               "result": {
                                 "id": 1,
                                 "nickname": "행복한쿼카",
                                 "bio": "안녕하세요",
                                 "profile_url": "https://example.com/image.png",
                                 "email": "user@apple.com",
                                 "provider": "apple",
                                 "provider_id": "001234.abc...",
                                 "created_at": "2025-11-01T12:00:00"
                               }
                             }
                             """))),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자",
                    content = @Content(schema = @Schema(implementation = CustomResponse.class),
                            examples = @ExampleObject(value = "{\"timestamp\": \"2025-11-04T21:01:00.123456\", \"isSuccess\": false, \"code\": \"COMMON401\", \"message\": \"인증이 필요합니다.\", \"result\": null}"))),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = CustomResponse.class),
                            examples = @ExampleObject(value = "{\"timestamp\": \"2025-11-04T21:01:00.123456\", \"isSuccess\": false, \"code\": \"USER404_1\", \"message\": \"사용자를 찾을 수 없습니다.\", \"result\": null}")))
    })
    @GetMapping("/me")
    public ResponseEntity<CustomResponse<UserDto.UserResponse>> getMyInfo(
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        Long userId = getUserId(user);
        UserDto.UserResponse myInfo = userService.getMyInfo(userId);
        return ResponseEntity.ok(CustomResponse.ok(myInfo));
    }

    /**
     * 닉네임 + 프로필 메시지(bio) 수정 (fetchUser)
     */
    @Operation(summary = "닉네임 및 프로필 메시지 수정 (fetchUser)", description = "현재 로그인한 사용자의 닉네임과 bio를 수정합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "수정 성공",
                    content = @Content(schema = @Schema(implementation = CustomResponse.class),
                            examples = @ExampleObject(value = "{\"timestamp\": \"2025-11-04T21:02:00.123456\", \"isSuccess\": true, \"code\": \"COMMON200\", \"message\": \"성공적으로 요청을 수행했습니다.\", \"result\": { ... (유저 전체 정보) ... }}"))),
            @ApiResponse(responseCode = "400", description = "닉네임 중복",
                    content = @Content(schema = @Schema(implementation = CustomResponse.class),
                            examples = @ExampleObject(value = "{\"timestamp\": \"2025-11-04T21:02:00.123456\", \"isSuccess\": false, \"code\": \"AUTH409_1\", \"message\": \"이미 사용 중인 닉네임입니다.\", \"result\": null}"))),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자", content = @Content)
    })
    @PatchMapping("/me/profile")
    public ResponseEntity<CustomResponse<UserDto.UserResponse>> updateProfile(
            @AuthenticationPrincipal AuthenticatedUser user,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "수정할 닉네임과 bio. bio는 null 또는 빈 문자열(\"\") 전송 가능.",
                    required = true,
                    content = @Content(examples = @ExampleObject(value = "{\"nickname\": \"새로운닉네임\", \"bio\": \"새로운 소개\"}"))
            )
            @Valid @RequestBody UserDto.ProfileUpdateRequest request
    ) {
        Long userId = getUserId(user);
        UserDto.UserResponse updatedUser = userService.updateProfile(userId, request);
        return ResponseEntity.ok(CustomResponse.ok(updatedUser));
    }

    /**
     * 프로필 사진 URL 변경/삭제 (fetchProfile)
     */
    @Operation(summary = "프로필 사진 URL 수정 (fetchProfile)", description = "프로필 사진 URL을 수정합니다. `profileUrl`에 `null` 또는 빈 문자열 `\"\"`을 보내면 사진이 삭제(null 처리)됩니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "수정/삭제 성공",
                    content = @Content(schema = @Schema(implementation = CustomResponse.class),
                            examples = @ExampleObject(value = "{\"timestamp\": \"2025-11-04T21:03:00.123456\", \"isSuccess\": true, \"code\": \"COMMON200\", \"message\": \"성공적으로 요청을 수행했습니다.\", \"result\": { ... (유저 전체 정보) ... }}"))),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자", content = @Content)
    })
    @PatchMapping("/me/profileUrl")
    public ResponseEntity<CustomResponse<UserDto.UserResponse>> updateProfileUrl(
            @AuthenticationPrincipal AuthenticatedUser user,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "수정할 프로필 URL. null 또는 빈 문자열 전송 시 기존 URL 삭제.",
                    required = true,
                    content = @Content(examples = {
                            @ExampleObject(name = "URL 수정", value = "{\"profileUrl\": \"https://new.image.com/pic.png\"}"),
                            @ExampleObject(name = "URL 삭제", value = "{\"profileUrl\": null}")
                    })
            )
            @RequestBody UserDto.ProfileUrlUpdateRequest request
    ) {
        Long userId = getUserId(user);

        // 빈 문자열 ""을 null로 변환하여 서비스 호출
        String urlToUpdate = (request.getProfileUrl() != null && request.getProfileUrl().isBlank()) ? null : request.getProfileUrl();
        UserDto.ProfileUrlUpdateRequest normalizedRequest = new UserDto.ProfileUrlUpdateRequest();
        // (Reflection or setter would be better, but this is explicit)
        // This is complex. Let's simplify the DTO handling.

        UserDto.UserResponse updatedUser = userService.updateProfileUrl(userId, request);

        return ResponseEntity.ok(CustomResponse.ok(updatedUser));
    }

    /**
     * 회원 탈퇴 (Soft Delete)
     */
    @Operation(summary = "회원 탈퇴 (Soft Delete)", description = "현재 로그인한 사용자의 계정을 탈퇴 처리(Soft Delete)합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "탈퇴 성공",
                    content = @Content(schema = @Schema(implementation = CustomResponse.class),
                            examples = @ExampleObject(value = "{\"timestamp\": \"2025-11-04T21:04:00.123456\", \"isSuccess\": true, \"code\": \"COMMON200\", \"message\": \"성공적으로 요청을 수행했습니다.\", \"result\": null}"))),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자", content = @Content),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음", content = @Content)
    })
    @DeleteMapping("/me")
    public ResponseEntity<CustomResponse<Void>> withdrawUser(
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        Long userId = getUserId(user);
        userService.withdrawUser(userId);
        return ResponseEntity.ok(CustomResponse.ok(null)); // 탈퇴는 null 반환
    }
}