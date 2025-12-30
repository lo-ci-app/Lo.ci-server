package com.teamloci.loci.domain.auth;

import com.teamloci.loci.global.common.CustomResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "인증 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "전화번호 로그인")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "요청 성공",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "code": "COMMON200",
                              "message": "성공적으로 요청을 수행했습니다.",
                              "result": {
                                "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
                                "isNewUser": false
                              }
                            }
                            """))),
            @ApiResponse(responseCode = "404", description = "전화번호 없음 (Firebase Token Invalid)", content = @Content)
    })
    @PostMapping("/login/phone")
    public ResponseEntity<CustomResponse<AuthResponse>> loginWithPhone(@Valid @RequestBody PhoneLoginRequest request) {
        AuthResponse authResponse = authService.loginWithPhone(request);
        return ResponseEntity.ok(CustomResponse.ok(authResponse));
    }

    @Operation(summary = "전화번호 회원가입")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "가입 성공 (Result: null)",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "code": "COMMON200",
                              "message": "성공적으로 요청을 수행했습니다.",
                              "result": null
                            }
                            """))),
            @ApiResponse(responseCode = "409", description = "이미 존재하는 유저/핸들", content = @Content)
    })
    @PostMapping("/signup/phone")
    public ResponseEntity<CustomResponse<Void>> signUpWithPhone(@Valid @RequestBody PhoneLoginRequest request) {
        authService.signUpWithPhone(request);
        return ResponseEntity.ok(CustomResponse.ok(null));
    }

    @Operation(summary = "토큰 재발급 (Reissue)", description = "Refresh Token을 사용하여 새로운 Access/Refresh Token을 발급받습니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "재발급 성공"),
            @ApiResponse(responseCode = "401", description = "유효하지 않거나 만료된 Refresh Token", content = @Content)
    })
    @PostMapping("/reissue")
    public ResponseEntity<CustomResponse<TokenResponse>> reissue(@RequestBody RefreshTokenRequest request) {
        TokenResponse tokenResponse = authService.reissue(request);
        return ResponseEntity.ok(CustomResponse.ok(tokenResponse));
    }
}