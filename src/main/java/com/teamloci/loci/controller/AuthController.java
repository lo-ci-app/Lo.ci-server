package com.teamloci.loci.controller;

import com.teamloci.loci.dto.AppleLoginRequest;
import com.teamloci.loci.dto.AuthResponse;
import com.teamloci.loci.global.response.CustomResponse;
import com.teamloci.loci.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "인증 및 로그인 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "[Auth] 1. Apple 로그인",
            description = "Apple ID 토큰을 사용하여 로그인 또는 회원가입을 처리하고, 서비스 JWT를 발급합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "로그인/회원가입 성공",
                    content = @Content(schema = @Schema(implementation = CustomResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "timestamp": "2025-11-04T20:00:00.123456",
                                      "isSuccess": true,
                                      "code": "COMMON200",
                                      "message": "성공적으로 요청을 수행했습니다.",
                                      "result": {
                                        "accessToken": "eyJh...[서비스 JWT]...",
                                        "isNewUser": true
                                      }
                                    }
                                    """))),
            @ApiResponse(responseCode = "400", description = "(COMMON400) identityToken이 누락된 경우", content = @Content),
            @ApiResponse(responseCode = "401", description = "(AUTH401_1) Apple ID 토큰 검증 실패", content = @Content)
    })
    @PostMapping("/login/apple")
    public ResponseEntity<CustomResponse<AuthResponse>> loginWithApple(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Apple에서 받은 로그인 정보",
                    required = true,
                    content = @Content(examples = @ExampleObject(value = "{\"identityToken\": \"eyJh...\", \"email\": \"user@apple.com\", \"fullName\": \"홍길동\"}"))
            )
            @RequestBody AppleLoginRequest request
    ) {
        AuthResponse authResponse = authService.loginWithApple(request);
        return ResponseEntity.ok(CustomResponse.ok(authResponse));
    }
}