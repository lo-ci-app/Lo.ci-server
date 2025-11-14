package com.teamfiv5.fiv5.service;

import com.teamfiv5.fiv5.config.jwt.JwtTokenProvider;
import com.teamfiv5.fiv5.domain.User;
import com.teamfiv5.fiv5.dto.AppleLoginRequest;
import com.teamfiv5.fiv5.dto.AuthResponse;
import com.teamfiv5.fiv5.global.exception.CustomException;
import com.teamfiv5.fiv5.global.exception.code.ErrorCode;
import com.teamfiv5.fiv5.global.util.RandomNicknameGenerator;
import com.teamfiv5.fiv5.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private static final String APPLE_PROVIDER = "apple";

    private static final SecureRandom random = new SecureRandom();
    private static final HexFormat hexFormat = HexFormat.of();

    @Transactional
    public AuthResponse loginWithApple(AppleLoginRequest request) {

        String providerId = request.getIdentityToken();

        if (!StringUtils.hasText(providerId)) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        Optional<User> userOptional = userRepository.findByProviderIdAndProvider(providerId, APPLE_PROVIDER);
        final boolean isActuallyNewUser = userOptional.isEmpty();
        User user;

        if (isActuallyNewUser) {
            String email = request.getEmail();
            String nickname;

            if (StringUtils.hasText(request.getFullName())) {
                nickname = request.getFullName();
            } else {
                nickname = RandomNicknameGenerator.generate();
            }

            if (userRepository.existsByNickname(nickname)) {
                nickname = nickname + "_" + providerId.substring(0, 4);
            }

            user = userRepository.save(
                    User.builder()
                            .email(email)
                            .nickname(nickname)
                            .provider(APPLE_PROVIDER)
                            .providerId(providerId)
                            .build()
            );
        } else {
            user = userOptional.get();
        }

        if (user.getBluetoothToken() == null) {
            user.updateBluetoothToken(generateUniqueBluetoothToken());
        }

        boolean treatAsNewUser = isActuallyNewUser || user.getNickname().startsWith("user_") || user.getNickname().startsWith("행복한 ") || user.getNickname().startsWith("즐거운 ");

        String accessToken = jwtTokenProvider.createAccessToken(user);
        return new AuthResponse(accessToken, treatAsNewUser);
    }

    private String generateUniqueBluetoothToken() {
        byte[] tokenBytes = new byte[4];
        String newToken;
        do {
            random.nextBytes(tokenBytes);
            newToken = hexFormat.formatHex(tokenBytes);
        } while (userRepository.existsByBluetoothToken(newToken));

        return newToken;
    }
}