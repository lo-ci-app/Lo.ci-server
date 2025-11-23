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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;
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
    public void sendFriendRequest(Long myUserId, Long targetUserId) {
        if (myUserId.equals(targetUserId)) throw new CustomException(ErrorCode.SELF_FRIEND_REQUEST);

        User me = findUserById(myUserId);
        User target = findUserById(targetUserId);

        Optional<Friendship> existing = friendshipRepository.findFriendshipBetween(myUserId, targetUserId);
        if (existing.isPresent()) {
            Friendship f = existing.get();
            if (f.getStatus() == FriendshipStatus.FRIENDSHIP) {
                throw new CustomException(ErrorCode.FRIEND_REQUEST_ALREADY_EXISTS);
            }
            if (f.getRequester().getId().equals(myUserId)) {
                throw new CustomException(ErrorCode.FRIEND_REQUEST_ALREADY_EXISTS);
            } else {
                if (friendshipRepository.countFriends(myUserId) >= MAX_FRIEND_LIMIT ||
                        friendshipRepository.countFriends(targetUserId) >= MAX_FRIEND_LIMIT) {
                    throw new CustomException(ErrorCode.FRIEND_LIMIT_EXCEEDED);
                }

                f.accept();
                return;
            }
        }

        if (friendshipRepository.countFriends(myUserId) >= MAX_FRIEND_LIMIT) {
            throw new CustomException(ErrorCode.FRIEND_LIMIT_EXCEEDED);
        }

        Friendship friendship = Friendship.builder()
                .requester(me)
                .receiver(target)
                .status(FriendshipStatus.PENDING)
                .build();

        friendshipRepository.save(friendship);

        if (StringUtils.hasText(target.getFcmToken())) {
            notificationService.sendFriendRequestNotification(target.getFcmToken(), me.getNickname());
        }
    }

    @Transactional
    public void acceptFriendRequest(Long myUserId, Long requesterId) {
        Friendship friendship = friendshipRepository.findFriendshipBetween(myUserId, requesterId)
                .orElseThrow(() -> new CustomException(ErrorCode.FRIEND_REQUEST_NOT_FOUND));

        if (!friendship.getReceiver().getId().equals(myUserId)) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        if (friendship.getStatus() == FriendshipStatus.FRIENDSHIP) {
            throw new CustomException(ErrorCode.FRIEND_REQUEST_ALREADY_EXISTS);
        }

        if (friendshipRepository.countFriends(myUserId) >= MAX_FRIEND_LIMIT ||
                friendshipRepository.countFriends(requesterId) >= MAX_FRIEND_LIMIT) {
            throw new CustomException(ErrorCode.FRIEND_LIMIT_EXCEEDED);
        }

        friendship.accept();
    }

    @Transactional
    public void deleteFriendship(Long myUserId, Long targetUserId) {
        Friendship friendship = friendshipRepository.findFriendshipBetween(myUserId, targetUserId)
                .orElseThrow(() -> new CustomException(ErrorCode.FRIEND_REQUEST_NOT_FOUND));

        friendshipRepository.delete(friendship);
    }

    public List<UserDto.UserResponse> getMyFriends(Long myUserId) {
        return friendshipRepository.findAllFriendsWithUsers(myUserId)
                .stream()
                .map(f -> f.getRequester().getId().equals(myUserId) ? f.getReceiver() : f.getRequester())
                .map(UserDto.UserResponse::from)
                .collect(Collectors.toList());
    }

    public List<UserDto.UserResponse> getReceivedRequests(Long myUserId) {
        return friendshipRepository.findReceivedRequests(myUserId).stream()
                .map(Friendship::getRequester)
                .map(UserDto.UserResponse::from)
                .collect(Collectors.toList());
    }

    public List<UserDto.UserResponse> getSentRequests(Long myUserId) {
        return friendshipRepository.findSentRequests(myUserId).stream()
                .map(Friendship::getReceiver)
                .map(UserDto.UserResponse::from)
                .collect(Collectors.toList());
    }

    public List<UserDto.UserResponse> searchUsers(String keyword, int page, int size) {
        if (keyword == null || keyword.isBlank()) return List.of();

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("id").descending());
        Slice<User> userSlice = userRepository.findByHandleContainingOrNicknameContaining(keyword, keyword, pageRequest);

        return userSlice.getContent().stream()
                .map(UserDto.UserResponse::from)
                .collect(Collectors.toList());
    }
}