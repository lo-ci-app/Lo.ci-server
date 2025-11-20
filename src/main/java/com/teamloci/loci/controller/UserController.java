package com.teamloci.loci.controller;

import com.teamloci.loci.dto.UserDto;
import com.teamloci.loci.global.exception.CustomException;
import com.teamloci.loci.global.exception.code.ErrorCode;
import com.teamloci.loci.global.response.CustomResponse;
import com.teamloci.loci.global.security.AuthenticatedUser;
import com.teamloci.loci.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "User", description = "사용자 프로필 및 설정 API")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    private Long getUserId(AuthenticatedUser user) {
        if (user == null) throw new CustomException(ErrorCode.UNAUTHORIZED);
        return user.getUserId();
    }

    @Operation(summary = "[User] 0. 닉네임 중복 검사 (가입/수정 전)",
            description = "입력한 닉네임이 사용 가능한지 확인합니다. (중복이면 false, 사용 가능하면 true 반환)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "확인 성공",
                    content = @Content(schema = @Schema(implementation = CustomResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "code": "COMMON200",
                                      "result": true
                                    }
                                    """)))
    })
    @GetMapping("/check-nickname")
    public ResponseEntity<CustomResponse<Boolean>> checkNickname(
            @Parameter(description = "검사할 닉네임", required = true, example = "행복한쿼카")
            @RequestParam String nickname
    ) {
        boolean isAvailable = userService.checkNicknameAvailability(nickname);
        return ResponseEntity.ok(CustomResponse.ok(isAvailable));
    }

    // ... (기존 getMyInfo, updateProfile, updateProfileUrl 등 나머지 메서드 그대로 유지) ...
    @GetMapping("/me")
    public ResponseEntity<CustomResponse<UserDto.UserResponse>> getMyInfo(
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        Long userId = getUserId(user);
        UserDto.UserResponse myInfo = userService.getMyInfo(userId);
        return ResponseEntity.ok(CustomResponse.ok(myInfo));
    }

    @Operation(summary = "[User] 2. 닉네임 및 프로필 메시지 수정",
            description = "현재 로그인한 사용자의 닉네임과 bio를 수정합니다.")
    @PatchMapping("/me/profile")
    public ResponseEntity<CustomResponse<UserDto.UserResponse>> updateProfile(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody UserDto.ProfileUpdateRequest request
    ) {
        Long userId = getUserId(user);
        UserDto.UserResponse updatedUser = userService.updateProfile(userId, request);
        return ResponseEntity.ok(CustomResponse.ok(updatedUser));
    }

    @Operation(summary = "[User] 3. 프로필 사진 업로드/변경/삭제 (MultipartFile)",
            description = "프로필 사진(이미지 파일)을 업로드합니다. 파일을 보내지 않으면(null) 기존 사진이 삭제됩니다.")
    @PatchMapping(value = "/me/profileUrl", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CustomResponse<UserDto.UserResponse>> updateProfileUrl(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestPart(value = "file", required = false) MultipartFile profileImage
    ) {
        Long userId = getUserId(user);
        UserDto.UserResponse updatedUser = userService.updateProfileUrl(userId, profileImage);
        return ResponseEntity.ok(CustomResponse.ok(updatedUser));
    }

    @Operation(summary = "[User] 4. 프로필 사진 URL 변경/삭제 (JSON)",
            description = "S3에 직접 업로드한 URL 문자열로 프로필 사진을 변경합니다. 빈 문자열 전송 시 삭제됩니다.")
    @PatchMapping(value = "/me/profileUrl", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CustomResponse<UserDto.UserResponse>> updateProfileUrlString(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody UserDto.ProfileUrlUpdateRequest request
    ) {
        Long userId = getUserId(user);
        UserDto.UserResponse updatedUser = userService.updateProfileUrl(userId, request);
        return ResponseEntity.ok(CustomResponse.ok(updatedUser));
    }

    @Operation(summary = "[User] 5. 회원 탈퇴 (Soft Delete)",
            description = "계정을 탈퇴 처리합니다.")
    @DeleteMapping("/me")
    public ResponseEntity<CustomResponse<Void>> withdrawUser(
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        Long userId = getUserId(user);
        userService.withdrawUser(userId);
        return ResponseEntity.ok(CustomResponse.ok(null));
    }

    @Operation(summary = "[User] 6. FCM 기기 토큰 갱신",
            description = "앱 실행/로그인 시 FCM 토큰을 서버에 등록합니다.")
    @PatchMapping("/me/fcm-token")
    public ResponseEntity<CustomResponse<Void>> updateFcmToken(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody UserDto.FcmTokenUpdateRequest request
    ) {
        Long userId = getUserId(user);
        userService.updateFcmToken(userId, request);
        return ResponseEntity.ok(CustomResponse.ok(null));
    }
}