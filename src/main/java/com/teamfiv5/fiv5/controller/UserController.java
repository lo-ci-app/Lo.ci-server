package com.teamfiv5.fiv5.controller;

import com.teamfiv5.fiv5.dto.UserDto;
import com.teamfiv5.fiv5.global.exception.CustomException;
import com.teamfiv5.fiv5.global.exception.code.ErrorCode;
import com.teamfiv5.fiv5.global.response.CustomResponse;
import com.teamfiv5.fiv5.global.security.AuthenticatedUser;
import com.teamfiv5.fiv5.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
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

import java.util.List;

@Tag(name = "User", description = "사용자 프로필 정보 API")
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
     * 5. 로그인한 유저 확인 API (GET /me)
     */
    @Operation(summary = "내 정보 조회 (로그인 유저 확인)",
            description = "현재 로그인한 사용자의 전체 프로필 정보를 조회합니다. 프론트엔드가 로그인 직후 토큰을 검증하고 사용자 정보를 가져오기 위해 사용합니다.")
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
                                 "profileUrl": "https://example.com/image.png",
                                 "email": "user@apple.com",
                                 "provider": "apple",
                                 "providerId": "001234.abc...",
                                 "createdAt": "2025-11-01T12:00:00"
                               }
                             }
                             """))),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자 (토큰 만료/없음)", content = @Content)
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
     * 3. 닉네임 + 프로필 메시지(bio) 수정 (patchUser)
     */
    @Operation(summary = "닉네임 및 프로필 메시지 수정 (patchUser)", description = "현재 로그인한 사용자의 닉네임과 bio를 수정합니다.")
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
     * 3. 프로필 사진 URL 변경/삭제 (patchProfile)
     * (수정) 4. 스웨거 어노테이션 수정
     */
    @Operation(summary = "프로필 사진 업로드/변경/삭제 (patchProfile)",
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

    /**
     * (신규) 4. 프로필 사진 [URL] 변경/삭제 (JSON)
     */
    @Operation(summary = "프로필 사진 [URL] 변경/삭제 (신규 방식)",
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

    /**
     * 회원 탈퇴 (Soft Delete)
     */
    @Operation(summary = "회원 탈퇴 (Soft Delete)",
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
        userService.withdrawUser(userId); // Soft Delete 실행
        return ResponseEntity.ok(CustomResponse.ok(null));
    }
}