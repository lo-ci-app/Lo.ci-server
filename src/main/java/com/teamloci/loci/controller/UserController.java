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

@Tag(name = "User", description = "사용자 프로필 정보 API")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    private Long getUserId(AuthenticatedUser user) {
        if (user == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
        return user.getUserId();
    }

    @Operation(summary = "[User] 0. 핸들(ID) 중복 검사",
            description = "입력한 핸들(@ID)이 사용 가능한지 확인합니다. (중복이면 false, 사용 가능하면 true)")
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
    @GetMapping("/check-handle")
    public ResponseEntity<CustomResponse<Boolean>> checkHandle(@RequestParam String handle) {
        boolean isAvailable = userService.checkHandleAvailability(handle);
        return ResponseEntity.ok(CustomResponse.ok(isAvailable));
    }

    @Operation(summary = "[User] 1. 내 정보 조회",
            description = "현재 로그인한 사용자의 프로필 정보를 조회합니다.")
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
                                 "handle": "happy_quokka",
                                 "nickname": "행복한쿼카",
                                 "profileUrl": "https://example.com/image.png",
                                 "createdAt": "2025-11-01T12:00:00"
                               }
                             }
                             """))),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content)
    })
    @GetMapping("/me")
    public ResponseEntity<CustomResponse<UserDto.UserResponse>> getMyInfo(
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        Long userId = getUserId(user);
        UserDto.UserResponse myInfo = userService.getMyInfo(userId);
        return ResponseEntity.ok(CustomResponse.ok(myInfo));
    }

    @Operation(summary = "[User] 2. 닉네임 및 프로필 메시지 수정 (patchUser)",
            description = "현재 로그인한 사용자의 닉네임과 bio를 수정합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "수정 성공",
                    content = @Content(schema = @Schema(implementation = CustomResponse.class),
                            examples = @ExampleObject(value = """
                             {
                               "timestamp": "2025-11-04T21:02:00.123456",
                               "isSuccess": true,
                               "code": "COMMON200",
                               "message": "성공적으로 요청을 수행했습니다.",
                               "result": {
                                 "id": 1,
                                 "nickname": "새로운닉네임",
                                 "bio": "새로운 소개",
                                 "profileUrl": "https://example.com/image.png",
                                 "email": "user@apple.com",
                                 "provider": "apple",
                                 "providerId": "001234.abc...",
                                 "createdAt": "2025-11-01T12:00:00"
                               }
                             }
                             """))),
            @ApiResponse(responseCode = "400", description = "(AUTH409_1) 닉네임 중복",
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

    @Operation(summary = "[User] 3. 프로필 사진 업로드/변경/삭제 (patchProfile)",
            description = "프로필 사진(이미지 파일)을 업로드합니다. **파일을 보내지 않으면(null)** 기존 사진이 **삭제**됩니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "수정/삭제 성공",
                    content = @Content(schema = @Schema(implementation = CustomResponse.class),
                            examples = @ExampleObject(value = """
                             {
                               "timestamp": "2025-11-04T21:03:00.123456",
                               "isSuccess": true,
                               "code": "COMMON200",
                               "message": "성공적으로 요청을 수행했습니다.",
                               "result": {
                                 "id": 1,
                                 "nickname": "이름",
                                 "bio": "안녕하세요",
                                 "profileUrl": "https://fiv5-assets.s3.ap-northeast-2.amazonaws.com/profiles/new-image.png",
                                 "email": "user@apple.com",
                                 "provider": "apple",
                                 "providerId": "001234.abc...",
                                 "createdAt": "2025-11-01T12:00:00"
                               }
                             }
                             """))),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자", content = @Content)
    })
    @PatchMapping(value = "/me/profileUrl", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CustomResponse<UserDto.UserResponse>> updateProfileUrl(
            @AuthenticationPrincipal AuthenticatedUser user,

            @Schema(
                    description = "업로드할 이미지 파일. 파일을 보내지 않으면 기존 사진이 삭제됩니다.",
                    type = "string",
                    format = "binary"
            )
            @RequestPart(value = "file", required = false) MultipartFile profileImage
    ) {
        Long userId = getUserId(user);
        UserDto.UserResponse updatedUser = userService.updateProfileUrl(userId, profileImage);
        return ResponseEntity.ok(CustomResponse.ok(updatedUser));
    }

    @Operation(summary = "[User] 4. 프로필 사진 [URL] 변경/삭제 (신규 방식)",
            description = "클라이언트가 S3에 직접 업로드한 후, 그 S3 URL 문자열만 서버로 전송합니다. **URL로 null이나 빈 문자열(\"\")을 보내면** 기존 사진이 **삭제**됩니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "URL 변경/삭제 성공",
                    content = @Content(schema = @Schema(implementation = CustomResponse.class),
                            examples = @ExampleObject(value = """
                             {
                               "timestamp": "2025-11-10T21:00:00.123456",
                               "isSuccess": true,
                               "code": "COMMON200",
                               "message": "성공적으로 요청을 수행했습니다.",
                               "result": {
                                 "id": 1,
                                 "profileUrl": "https://fiv5-assets.s3.ap-northeast-2.amazonaws.com/profiles/new-image-url.png",
                                 ...
                               }
                             }
                             """))),
            @ApiResponse(responseCode = "401", description = "(COMMON401) 인증되지 않은 사용자", content = @Content)
    })
    @PatchMapping(value = "/me/profileUrl", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CustomResponse<UserDto.UserResponse>> updateProfileUrlString(
            @AuthenticationPrincipal AuthenticatedUser user,

            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "S3에 업로드된 새 프로필 URL. null 또는 빈 문자열 전송 시 기존 사진 삭제.",
                    required = true,
                    content = @Content(examples = @ExampleObject(value = "{\"profileUrl\": \"https.../new-image.png\"}"))
            )
            @Valid @RequestBody UserDto.ProfileUrlUpdateRequest request
    ) {
        Long userId = getUserId(user);
        UserDto.UserResponse updatedUser = userService.updateProfileUrl(userId, request);
        return ResponseEntity.ok(CustomResponse.ok(updatedUser));
    }

    @Operation(summary = "[User] 5. 회원 탈퇴 (Soft Delete)",
            description = "현재 로그인한 사용자의 계정을 탈퇴 처리(Soft Delete)합니다. (DB 레코드 유지)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "탈퇴 성공 (Soft Delete)", content = @Content(/* ... */)),
            @ApiResponse(responseCode = "401", description = "(COMMON401) 인증되지 않은 사용자", content = @Content)
    })
    @DeleteMapping("/me")
    public ResponseEntity<CustomResponse<Void>> withdrawUser(
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        Long userId = getUserId(user);
        userService.withdrawUser(userId);
        return ResponseEntity.ok(CustomResponse.ok(null));
    }

    @Operation(summary = "[User] 6. FCM 기기 토큰 갱신",
            description = "로그인 시 또는 기기 토큰이 갱신될 때마다 클라이언트가 이 API를 호출하여 서버에 토큰을 등록합니다.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "FCM 기기 토큰",
            required = true,
            content = @Content(examples = @ExampleObject(value = "{\"fcmToken\": \"c_abc123...\"}"))
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "FCM 토큰 갱신 성공", content = @Content),
            @ApiResponse(responseCode = "401", description = "(COMMON401) 인증 실패", content = @Content)
    })
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