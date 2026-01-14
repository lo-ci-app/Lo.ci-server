package com.teamloci.loci.domain.admin;

import com.teamloci.loci.domain.auth.AuthResponse;
import com.teamloci.loci.domain.user.User;
import com.teamloci.loci.domain.user.UserRepository;
import com.teamloci.loci.global.auth.JwtTokenProvider;
import com.teamloci.loci.global.common.CustomResponse;
import com.teamloci.loci.global.error.CustomException;
import com.teamloci.loci.global.error.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin", description = "관리자/테스트용 API")
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Operation(summary = "테스트용 JWT 토큰 발급", description = "유저 ID를 입력하면 해당 유저의 Access/Refresh Token을 즉시 발급합니다. (비밀번호/인증 없음)")
    @GetMapping("/token")
    public ResponseEntity<CustomResponse<AuthResponse>> generateToken(
            @Parameter(description = "토큰을 생성할 유저의 ID", example = "1", required = true)
            @RequestParam Long userId
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        String accessToken = jwtTokenProvider.createAccessToken(user);
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());

        AuthResponse response = new AuthResponse(accessToken, refreshToken, false);

        return ResponseEntity.ok(CustomResponse.ok(response));
    }
}