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

    public boolean checkHandleAvailability(String handle) {
        return !userRepository.existsByHandle(handle);
    }

    public UserDto.UserResponse getMyInfo(Long userId) {
        User user = findUserById(userId);
        return UserDto.UserResponse.of(user, "SELF");
    }

    @Transactional
    public UserDto.UserResponse updateProfile(Long userId, UserDto.ProfileUpdateRequest request) {
        User user = findUserById(userId);

        String newHandle = request.getHandle();
        String newNickname = request.getNickname();

        if (newHandle != null && !newHandle.equals(user.getHandle())) {
            if (userRepository.existsByHandle(newHandle)) {
                throw new CustomException(ErrorCode.HANDLE_DUPLICATED);
            }
        } else if (newHandle == null) {
            newHandle = user.getHandle();
        }

        if (newNickname == null) {
            newNickname = user.getNickname();
        }

        user.updateProfile(newHandle, newNickname);

        return UserDto.UserResponse.of(user, "SELF");
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
        return UserDto.UserResponse.of(user, "SELF");
    }

    @Transactional
    public UserDto.UserResponse updateProfileUrl(Long userId, UserDto.ProfileUrlUpdateRequest request) {
        User user = findUserById(userId);
        String oldFileUrl = user.getProfileUrl();
        String newFileUrl = request.getProfileUrl();

        s3UploadService.replaceUrl(newFileUrl, oldFileUrl);

        user.updateProfileUrl(newFileUrl);
        return UserDto.UserResponse.of(user, "SELF");
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
}