// 경로: src/main/java/com/teamfiv5/fiv5/service/UserService.java
package com.teamfiv5.fiv5.service;

import com.teamfiv5.fiv5.domain.User;
import com.teamfiv5.fiv5.dto.UserDto;
import com.teamfiv5.fiv5.global.exception.CustomException;
import com.teamfiv5.fiv5.global.exception.code.ErrorCode;
import com.teamfiv5.fiv5.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final S3UploadService s3UploadService;
    private static final SecureRandom random = new SecureRandom();

    // 공용: 사용자 ID로 User 엔티티 조회 (없으면 예외)
    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    // 내 정보 조회
    public UserDto.UserResponse getMyInfo(Long userId) {
        User user = findUserById(userId);
        return UserDto.UserResponse.from(user);
    }

    // 기능 1: 닉네임 + bio 변경 (fetchUser)
    @Transactional
    public UserDto.UserResponse updateProfile(Long userId, UserDto.ProfileUpdateRequest request) {
        User user = findUserById(userId);
        String newNickname = request.getNickname();
        String newBio = request.getBio();

        // 닉네임이 현재 닉네임과 다를 때만 중복 검사
        if (!user.getNickname().equals(newNickname)) {
            if (userRepository.existsByNickname(newNickname)) {
                throw new CustomException(ErrorCode.NICKNAME_DUPLICATED);
            }
        }

        user.updateProfile(newNickname, newBio); // 닉네임과 bio 동시 업데이트
        return UserDto.UserResponse.from(user); // 수정된 유저 정보 반환
    }

    /**
     * [파일]을 직접 받아 S3 업로드/변경/삭제 (MultipartFile)
     * S3UploadService의 공통 로직 호출
     */
    @Transactional
    public UserDto.UserResponse updateProfileUrl(Long userId, MultipartFile profileImage) {
        User user = findUserById(userId);

        String newFileUrl = s3UploadService.uploadAndReplace(
                profileImage,         // 새 파일
                user.getProfileUrl(), // 기존 URL
                "profiles"            // S3 디렉토리
        );

        user.updateProfileUrl(newFileUrl); // DB 업데이트
        return UserDto.UserResponse.from(user);
    }

    /**
     * 기능 2-1: S3 https://namu.wiki/w/%EB%AC%B8%EC%9E%90%EC%97%B4을 받아 DB에 저장/변경/삭제
     * S3UploadService의 공통 로직 호출
     */
    @Transactional
    public UserDto.UserResponse updateProfileUrl(Long userId, UserDto.ProfileUrlUpdateRequest request) {
        User user = findUserById(userId);
        String oldFileUrl = user.getProfileUrl();
        String newFileUrl = request.getProfileUrl();

        s3UploadService.replaceUrl(newFileUrl, oldFileUrl);

        user.updateProfileUrl(newFileUrl); // DB 업데이트
        return UserDto.UserResponse.from(user);
    }

    /**
     * 기능 3: 회원 탈퇴 (Hard Delete -> Soft Delete)
     * (수정) userRepository.delete() 대신 user.withdraw() 호출
     */
    @Transactional
    public void withdrawUser(Long userId) {
        User user = findUserById(userId);

        // (복구) 소프트 딜리트 로직으로 원상복구
        user.withdraw();
    }
}