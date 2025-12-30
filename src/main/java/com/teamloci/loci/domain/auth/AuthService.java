package com.teamloci.loci.domain.auth;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.teamloci.loci.domain.user.User;
import com.teamloci.loci.domain.user.UserRepository;
import com.teamloci.loci.global.auth.JwtTokenProvider;
import com.teamloci.loci.global.error.CustomException;
import com.teamloci.loci.global.error.ErrorCode;
import com.teamloci.loci.global.util.AesUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final AesUtil aesUtil;
    private final StringRedisTemplate redisTemplate;

    private static final SecureRandom secureRandom = new SecureRandom();
    private static final HexFormat hexFormat = HexFormat.of();

    private static final String REFRESH_TOKEN_PREFIX = "RT:";

    @Transactional
    public AuthResponse loginWithPhone(PhoneLoginRequest request) {
        String phoneNumber = verifyFirebaseToken(request.getIdToken());
        String searchHash = aesUtil.hash(phoneNumber);

        return userRepository.findByPhoneSearchHash(searchHash)
                .map(user -> {
                    String accessToken = jwtTokenProvider.createAccessToken(user);
                    String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());

                    storeRefreshToken(user.getId(), refreshToken);

                    return new AuthResponse(accessToken, refreshToken, false);
                })
                .orElseGet(() -> new AuthResponse(null, null, true));
    }

    @Transactional
    public TokenResponse reissue(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }

        String userId = jwtTokenProvider.getUserIdFromToken(refreshToken);

        String storedToken = redisTemplate.opsForValue().get(REFRESH_TOKEN_PREFIX + userId);
        if (storedToken == null || !storedToken.equals(refreshToken)) {
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        User user = userRepository.findById(Long.parseLong(userId))
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        String newAccessToken = jwtTokenProvider.createAccessToken(user);
        String newRefreshToken = jwtTokenProvider.createRefreshToken(user.getId());

        storeRefreshToken(user.getId(), newRefreshToken);

        return new TokenResponse(newAccessToken, newRefreshToken);
    }

    private void storeRefreshToken(Long userId, String refreshToken) {
        redisTemplate.opsForValue().set(
                REFRESH_TOKEN_PREFIX + userId,
                refreshToken,
                jwtTokenProvider.getRefreshTokenValidityInMilliseconds(),
                TimeUnit.MILLISECONDS
        );
    }

    @Transactional
    public void signUpWithPhone(PhoneLoginRequest request) {
        validateSignUpRequest(request);

        String phoneNumber = verifyFirebaseToken(request.getIdToken());
        String searchHash = aesUtil.hash(phoneNumber);
        String encryptedPhone = aesUtil.encrypt(phoneNumber);

        validateDuplicateUser(request.getHandle(), searchHash);

        User newUser = User.builder()
                .handle(request.getHandle())
                .nickname(request.getNickname())
                .countryCode(StringUtils.hasText(request.getCountryCode()) ? request.getCountryCode() : "KR")
                .phoneEncrypted(encryptedPhone)
                .phoneSearchHash(searchHash)
                .build();

        newUser.updateBluetoothToken(generateUniqueBluetoothToken());

        userRepository.save(newUser);
        log.info(">>> [회원가입 완료] User ID: {}, Handle: {}", newUser.getId(), newUser.getHandle());
    }

    private void validateSignUpRequest(PhoneLoginRequest request) {
        if (!StringUtils.hasText(request.getHandle()) || !StringUtils.hasText(request.getNickname())) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
    }

    private void validateDuplicateUser(String handle, String searchHash) {
        if (userRepository.findByPhoneSearchHash(searchHash).isPresent()) {
            throw new CustomException(ErrorCode.PHONE_NUMBER_ALREADY_USING);
        }
        if (userRepository.existsByHandle(handle)) {
            throw new CustomException(ErrorCode.HANDLE_DUPLICATED);
        }
    }

    private String verifyFirebaseToken(String idToken) {
        try {
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
            String phoneNumber = (String) decodedToken.getClaims().get("phone_number");

            if (!StringUtils.hasText(phoneNumber)) {
                throw new CustomException(ErrorCode.NO_PHONE_NUMBER);
            }
            return phoneNumber;
        } catch (FirebaseAuthException e) {
            log.error("Firebase 토큰 검증 실패: code={}, msg={}", e.getErrorCode(), e.getMessage());
            throw new CustomException(ErrorCode.FIREBASE_AUTH_FAILED);
        }
    }

    private String generateUniqueBluetoothToken() {
        byte[] tokenBytes = new byte[4];
        String token;
        do {
            secureRandom.nextBytes(tokenBytes);
            token = hexFormat.formatHex(tokenBytes);
        } while (userRepository.existsByBluetoothToken(token));
        return token;
    }
}