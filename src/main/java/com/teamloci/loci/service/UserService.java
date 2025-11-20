package com.teamloci.loci.service;

import com.teamloci.loci.domain.User;
import com.teamloci.loci.dto.UserDto;
import com.teamloci.loci.global.exception.CustomException;
import com.teamloci.loci.global.exception.code.ErrorCode;
import com.teamloci.loci.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.security.SecureRandom;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final S3UploadService s3UploadService;
    private static final SecureRandom random = new SecureRandom();

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    public UserDto.UserResponse getMyInfo(Long userId) {
        User user = findUserById(userId);
        return UserDto.UserResponse.from(user);
    }

    @Transactional
    public UserDto.UserResponse updateProfile(Long userId, UserDto.ProfileUpdateRequest request) {
        User user = findUserById(userId);
        String newNickname = request.getNickname();
        String newBio = request.getBio();

        if (!user.getNickname().equals(newNickname)) {
            if (userRepository.existsByNickname(newNickname)) {
                throw new CustomException(ErrorCode.NICKNAME_DUPLICATED);
            }
        }

        user.updateProfile(newNickname, newBio);
        return UserDto.UserResponse.from(user);
    }

    @Transactional
    public UserDto.UserResponse updateProfileUrl(Long userId, MultipartFile profileImage) {
        User user = findUserById(userId);

        String newFileUrl = s3UploadService.uploadAndReplace(
                profileImage,
                user.getProfileUrl(),
                "profiles"
        );

        user.updateProfileUrl(newFileUrl);
        return UserDto.UserResponse.from(user);
    }

    @Transactional
    public UserDto.UserResponse updateProfileUrl(Long userId, UserDto.ProfileUrlUpdateRequest request) {
        User user = findUserById(userId);
        String oldFileUrl = user.getProfileUrl();
        String newFileUrl = request.getProfileUrl();

        s3UploadService.replaceUrl(newFileUrl, oldFileUrl);

        user.updateProfileUrl(newFileUrl);
        return UserDto.UserResponse.from(user);
    }

    @Transactional
    public void withdrawUser(Long userId) {
        User user = findUserById(userId);
        user.withdraw();
    }

    @Transactional
    public void updateFcmToken(Long userId, UserDto.FcmTokenUpdateRequest request) {
        User user = findUserById(userId);
        user.updateFcmToken(request.getFcmToken());
    }

    public boolean checkNicknameAvailability(String nickname) {
        return !userRepository.existsByNickname(nickname);
    }
}