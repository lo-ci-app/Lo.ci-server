package com.teamfiv5.fiv5.service;

import com.teamfiv5.fiv5.config.apple.AppleTokenVerifier;
import com.teamfiv5.fiv5.config.jwt.JwtTokenProvider;
import com.teamfiv5.fiv5.domain.User;
import com.teamfiv5.fiv5.dto.AppleLoginRequest;
import com.teamfiv5.fiv5.dto.AuthResponse;
import com.teamfiv5.fiv5.dto.TokenResponse;
import com.teamfiv5.fiv5.global.exception.CustomException;
import com.teamfiv5.fiv5.global.exception.code.ErrorCode;
import com.teamfiv5.fiv5.global.util.RandomNicknameGenerator;
import com.teamfiv5.fiv5.repository.UserRepository;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private static final String APPLE_PROVIDER = "apple";

    @Transactional
    public AuthResponse loginWithApple(AppleLoginRequest request) {

        // 2. DB에서 사용자 조회
        String providerId = request.getIdentityToken();

        if (!StringUtils.hasText(providerId)) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        boolean isNewUser = false;

        User user = userRepository.findByProviderIdAndProvider(providerId, APPLE_PROVIDER)
                .orElseGet(() -> {
                    String email = request.getEmail();
                    String nickname;

                    if (StringUtils.hasText(request.getFullName())) {
                        nickname = request.getFullName();
                    } else {
                        // 이름이 없으면 랜덤 닉네임 생성
                        nickname = RandomNicknameGenerator.generate();
                    }

                    // (필수) 랜덤 닉네임 중복 시 임시 처리
                    if (userRepository.existsByNickname(nickname)) {
                        nickname = nickname + "_" + providerId.substring(0, 4);
                    }

                    return userRepository.save(
                            User.builder()
                                    .email(email)
                                    .nickname(nickname)
                                    .provider(APPLE_PROVIDER)
                                    .providerId(providerId)
                                    .build()
                    );
                });

        // Apple은 fullName을 안 줄 수 있으므로, 임시 닉네임 형태인지도 검사
        if (user.getNickname().startsWith("user_") || user.getNickname().startsWith("행복한 ") || user.getNickname().startsWith("즐거운 ")) {
            isNewUser = true;
        }

        String accessToken = jwtTokenProvider.createAccessToken(user);
        return new AuthResponse(accessToken, isNewUser);
    }

//    @Transactional(readOnly = true)
//    public TokenResponse refreshAccessToken(RefreshTokenRequest request) {
//        String refreshToken = request.getRefreshToken();
//
//        if (!jwtTokenProvider.validateToken(refreshToken)) {
//            throw new CustomException(ErrorCode.UNAUTHORIZED);
//        }
//
//        String userIdStr = jwtTokenProvider.getUserIdFromToken(refreshToken);
//        Long userId = Long.parseLong(userIdStr);
//
//        User user = userRepository.findById(userId)
//                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
//
//        String newAccessToken = jwtTokenProvider.createAccessToken(user);
//
//        return new TokenResponse(newAccessToken, refreshToken);
//    }
}