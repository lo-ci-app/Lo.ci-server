package com.teamloci.loci.domain.user;

import com.teamloci.loci.domain.friend.Friendship;
import com.teamloci.loci.domain.friend.FriendshipStatus;
import com.teamloci.loci.domain.user.service.UserActivityService;
import com.teamloci.loci.global.error.CustomException;
import com.teamloci.loci.global.error.ErrorCode;
import com.teamloci.loci.global.infra.S3UploadService;
import com.teamloci.loci.domain.friend.FriendshipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final S3UploadService s3UploadService;
    private final FriendshipRepository friendshipRepository;
    private final UserActivityService userActivityService;

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    public boolean checkHandleAvailability(String handle) {
        return !userRepository.existsByHandle(handle);
    }

    public UserDto.UserResponse getUserProfile(Long myUserId, Long targetUserId) {
        User targetUser = findUserById(targetUserId);

        var stats = userActivityService.getUserStats(targetUserId);

        String relationStatus = "NONE";
        if (myUserId.equals(targetUserId)) {
            relationStatus = "SELF";
        } else {
            Optional<Friendship> friendship = friendshipRepository.findFriendshipBetween(myUserId, targetUserId);
            if (friendship.isPresent()) {
                Friendship f = friendship.get();
                if (f.getStatus() == FriendshipStatus.FRIENDSHIP) {
                    relationStatus = "FRIEND";
                } else if (f.getRequester().getId().equals(myUserId)) {
                    relationStatus = "PENDING_SENT";
                } else {
                    relationStatus = "PENDING_RECEIVED";
                }
            }
        }

        return UserDto.UserResponse.of(
                targetUser,
                relationStatus,
                stats.friendCount(),
                stats.postCount(),
                stats.streakCount(),
                stats.visitedPlaceCount()
        );
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

        if (request.getIsAutoArchive() != null) {
            user.updateAutoArchive(request.getIsAutoArchive());
        }

        user.updateProfile(newHandle, newNickname);

        return getUserProfile(userId, userId);
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
        return getUserProfile(userId, userId);
    }

    @Transactional
    public UserDto.UserResponse updateProfileUrl(Long userId, UserDto.ProfileUrlUpdateRequest request) {
        User user = findUserById(userId);
        String oldFileUrl = user.getProfileUrl();
        String newFileUrl = request.getProfileUrl();

        s3UploadService.replaceUrl(newFileUrl, oldFileUrl);

        user.updateProfileUrl(newFileUrl);

        return getUserProfile(userId, userId);
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