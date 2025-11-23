package com.teamloci.loci.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.teamloci.loci.dto.AuthResponse;
import com.teamloci.loci.dto.PhoneLoginRequest;
import com.teamloci.loci.global.response.CustomResponse;
import com.teamloci.loci.service.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Auth", description = "인증 및 로그인 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "[Auth] 1. 전화번호 로그인 (가입 확인 및 토큰 발급)",
            description = "Firebase ID Token을 보내 가입 여부를 확인합니다. \n\n" +
                    "* **기존 회원:** `accessToken` 발급, `isNewUser: false`\n" +
                    "* **신규 회원:** `accessToken: null`, `isNewUser: true` 반환 -> **회원가입 API 호출 필요**")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "확인 성공",
                    content = @Content(schema = @Schema(implementation = CustomResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "code": "COMMON200",
                                      "result": {
                                        "accessToken": "eyJh...",
                                        "isNewUser": false
                                      }
                                    }
                                    """)))
    })
    @PostMapping("/login/phone")
    public ResponseEntity<CustomResponse<AuthResponse>> loginWithPhone(
            @Valid @RequestBody PhoneLoginRequest request
    ) {
        AuthResponse authResponse = authService.loginWithPhone(request);
        return ResponseEntity.ok(CustomResponse.ok(authResponse));
    }

    @Operation(summary = "[Auth] 2. 전화번호 회원가입 (계정 생성)",
            description = "신규 유저의 계정을 생성합니다. **토큰을 반환하지 않으므로**, 가입 성공 후 다시 **로그인 API**를 호출하여 토큰을 받아야 합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "가입 성공 (토큰 없음)",
                    content = @Content(schema = @Schema(implementation = CustomResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "code": "COMMON200",
                                      "result": null
                                    }
                                    """))),
            @ApiResponse(responseCode = "409", description = "이미 가입된 사용자", content = @Content)
    })
    @PostMapping("/signup/phone")
    public ResponseEntity<CustomResponse<Void>> signUpWithPhone(
            @Valid @RequestBody PhoneLoginRequest request
    ) {
        authService.signUpWithPhone(request);
        return ResponseEntity.ok(CustomResponse.ok(null));
    }
}