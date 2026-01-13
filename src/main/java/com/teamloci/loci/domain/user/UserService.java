package com.teamloci.loci.domain.user;

import com.teamloci.loci.domain.badge.UserBadgeRepository;
import com.teamloci.loci.domain.friend.Friendship;
import com.teamloci.loci.domain.friend.FriendshipRepository;
import com.teamloci.loci.domain.intimacy.entity.FriendshipIntimacy;
import com.teamloci.loci.domain.intimacy.repository.FriendshipIntimacyRepository;
import com.teamloci.loci.domain.intimacy.repository.IntimacyLevelRepository;
import com.teamloci.loci.domain.intimacy.repository.IntimacyLogRepository;
import com.teamloci.loci.domain.notification.NotificationRepository;
import com.teamloci.loci.domain.post.repository.PostCommentRepository;
import com.teamloci.loci.domain.post.repository.PostRepository;
import com.teamloci.loci.global.error.CustomException;
import com.teamloci.loci.global.error.ErrorCode;
import com.teamloci.loci.global.infra.S3UploadService;
import com.teamloci.loci.global.util.RelationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final S3UploadService s3UploadService;
    private final FriendshipRepository friendshipRepository;
    private final UserActivityService userActivityService;
    private final FriendshipIntimacyRepository intimacyRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final FriendshipIntimacyRepository friendshipIntimacyRepository;
    private final PostCommentRepository postCommentRepository;
    private final NotificationRepository notificationRepository;
    private final IntimacyLogRepository intimacyLogRepository;
    private final PostRepository postRepository;

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
            relationStatus = RelationUtil.resolveStatus(friendship.orElse(null), myUserId);
        }

        UserDto.UserResponse response = UserDto.UserResponse.of(
                targetUser,
                relationStatus,
                stats.friendCount(),
                stats.postCount(),
                stats.streakCount(),
                stats.visitedPlaceCount()
        );

        FriendshipIntimacy intimacy = null;
        if ("FRIEND".equals(relationStatus)) {
            Long u1 = Math.min(myUserId, targetUserId);
            Long u2 = Math.max(myUserId, targetUserId);
            intimacy = intimacyRepository.findByUserAIdAndUserBId(u1, u2).orElse(null);
        }

        response.applyIntimacyInfo(intimacy, stats.totalIntimacyLevel());

        return response;
    }

    public List<UserDto.UserResponse> getUserList(Long myUserId, List<String> bluetoothTokens) {
        if (bluetoothTokens == null || bluetoothTokens.isEmpty()) {
            return List.of();
        }

        List<User> users = userRepository.findByBluetoothTokenIn(bluetoothTokens);

        if (users.isEmpty()) {
            return List.of();
        }

        List<Long> targetUserIds = users.stream().map(User::getId).toList();

        List<Friendship> friendships = friendshipRepository.findAllRelationsBetween(myUserId, targetUserIds);
        Map<Long, Friendship> friendshipMap = friendships.stream()
                .collect(Collectors.toMap(
                        f -> f.getRequester().getId().equals(myUserId) ? f.getReceiver().getId() : f.getRequester().getId(),
                        f -> f,
                        (existing, replacement) -> {
                            log.warn("Duplicate friendship found for user {}. existing: {}, replacement: {}",
                                    existing.getRequester().getId().equals(myUserId) ? existing.getReceiver().getId() : existing.getRequester().getId(),
                                    existing.getId(), replacement.getId());
                            return existing;
                        }
                ));

        Map<Long, UserActivityService.UserStats> statsMap = userActivityService.getUserStatsMap(targetUserIds);

        List<FriendshipIntimacy> intimacies = intimacyRepository.findByUserIdAndTargetIdsIn(myUserId, targetUserIds);
        Map<Long, FriendshipIntimacy> intimacyMap = intimacies.stream()
                .collect(Collectors.toMap(
                        fi -> fi.getUserAId().equals(myUserId) ? fi.getUserBId() : fi.getUserAId(),
                        fi -> fi,
                        (existing, replacement) -> {
                            log.warn("Duplicate intimacy found. existing: {}, replacement: {}", existing.getId(), replacement.getId());
                            return existing;
                        }
                ));

        return users.stream().map(targetUser -> {
            Long targetId = targetUser.getId();

            var stats = statsMap.getOrDefault(targetId, new UserActivityService.UserStats(0, 0, 0, 0, 0));

            String relationStatus = "NONE";
            FriendshipIntimacy intimacy = null;

            if (myUserId.equals(targetId)) {
                relationStatus = "SELF";
            } else {
                Friendship friendship = friendshipMap.get(targetId);
                relationStatus = RelationUtil.resolveStatus(friendship, myUserId);

                if ("FRIEND".equals(relationStatus)) {
                    intimacy = intimacyMap.get(targetId);
                }
            }

            UserDto.UserResponse response = UserDto.UserResponse.of(
                    targetUser,
                    relationStatus,
                    stats.friendCount(),
                    stats.postCount(),
                    stats.streakCount(),
                    stats.visitedPlaceCount()
            );

            response.applyIntimacyInfo(intimacy, stats.totalIntimacyLevel());

            return response;
        }).collect(Collectors.toList());
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

        if (request.getCountryCode() != null) {
            user.updateCountryCode(request.getCountryCode());
        }

        if (request.getTimezone() != null && !request.getTimezone().isBlank()) {
            user.updateTimezone(request.getTimezone());
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
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        log.info(">>> 회원 탈퇴 진행: User ID {}", userId);

        userBadgeRepository.deleteByUser(user);
        user.updateMainBadge(null);

        friendshipRepository.deleteByFromUserOrToUser(user, user);

        friendshipIntimacyRepository.deleteByUserAIdOrUserBId(userId, userId);
        
        intimacyLogRepository.deleteByActorIdOrTargetId(userId, userId);

        notificationRepository.deleteByReceiver(user);

        postCommentRepository.deleteByUser(user);

        postRepository.deleteByUser(user);

        userRepository.delete(user);

        log.info(">>> 회원 탈퇴 완료: User ID {}", userId);
    }

    @Transactional
    public void updateFcmToken(Long userId, UserDto.FcmTokenUpdateRequest request) {
        User user = findUserById(userId);
        user.updateFcmToken(request.getFcmToken());
    }

    public UserDto.BluetoothTokenResponse getMyBluetoothToken(Long userId) {
        User user = findUserById(userId);
        return new UserDto.BluetoothTokenResponse(user.getBluetoothToken());
    }
}