package com.teamloci.loci.service;

import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.teamloci.loci.domain.Friendship;
import com.teamloci.loci.domain.FriendshipStatus;
import com.teamloci.loci.domain.User;
import com.teamloci.loci.domain.UserStatus;
import com.teamloci.loci.dto.UserDto;
import com.teamloci.loci.global.exception.CustomException;
import com.teamloci.loci.global.exception.code.ErrorCode;
import com.teamloci.loci.global.util.AesUtil;
import com.teamloci.loci.repository.FriendshipRepository;
import com.teamloci.loci.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class FriendService {

    private static final int MAX_FRIEND_LIMIT = 20;

    private final UserRepository userRepository;
    private final FriendshipRepository friendshipRepository;
    private final NotificationService notificationService;
    private final AesUtil aesUtil;

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    public List<UserDto.UserResponse> matchFriends(Long myUserId, List<String> rawPhoneNumbers) {
        User me = findUserById(myUserId);
        String defaultRegion = StringUtils.hasText(me.getCountryCode()) ? me.getCountryCode() : "KR";
        PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();

        List<String> hashedNumbers = rawPhoneNumbers.stream()
                .map(rawNumber -> {
                    try {
                        var parsedNumber = phoneUtil.parse(rawNumber, defaultRegion);
                        String e164Number = phoneUtil.format(parsedNumber, PhoneNumberUtil.PhoneNumberFormat.E164);
                        return aesUtil.hash(e164Number);
                    } catch (Exception e) { return null; }
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());

        if (hashedNumbers.isEmpty()) return List.of();

        return userRepository.findByPhoneSearchHashIn(hashedNumbers).stream()
                .filter(user -> !user.getId().equals(myUserId))
                .filter(user -> user.getStatus() == UserStatus.ACTIVE)
                .map(UserDto.UserResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public void addFriend(Long myUserId, Long targetUserId) {
        if (myUserId.equals(targetUserId)) throw new CustomException(ErrorCode.SELF_FRIEND_REQUEST);

        User me = findUserById(myUserId);
        User target = findUserById(targetUserId);

        // 이미 친구인지 확인
        if (friendshipRepository.existsFriendshipBetween(myUserId, targetUserId)) {
            throw new CustomException(ErrorCode.FRIEND_REQUEST_ALREADY_EXISTS);
        }

        // 친구 수 제한 체크
        long myCount = friendshipRepository.countByUserIdAndStatus(myUserId, FriendshipStatus.FRIENDSHIP);
        if (myCount >= MAX_FRIEND_LIMIT) throw new CustomException(ErrorCode.FRIEND_LIMIT_EXCEEDED);

        // 바로 맞팔(FRIENDSHIP) 상태로 저장 (단방향/양방향 정책에 따라 다르지만, 보통 이런 앱은 양방향)
        Friendship friendship = Friendship.builder()
                .requester(me)
                .receiver(target)
                .status(FriendshipStatus.FRIENDSHIP) // PENDING 아님
                .build();

        friendshipRepository.save(friendship);

        if (StringUtils.hasText(target.getFcmToken())) {
            notificationService.sendFriendRequestNotification(target.getFcmToken(), me.getNickname());
        }
    }

    public List<UserDto.UserResponse> getMyFriends(Long myUserId) {
        return friendshipRepository.findAllFriendsWithUsers(myUserId, FriendshipStatus.FRIENDSHIP)
                .stream()
                .map(f -> f.getRequester().getId().equals(myUserId) ? f.getReceiver() : f.getRequester())
                .map(UserDto.UserResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteFriend(Long myUserId, Long friendId) {
        Friendship friendship = friendshipRepository
                .findFriendshipBetweenUsersByStatus(myUserId, friendId, FriendshipStatus.FRIENDSHIP)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FRIENDS));
        friendshipRepository.delete(friendship);
    }
}