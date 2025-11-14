package com.teamfiv5.fiv5.service;

import com.teamfiv5.fiv5.domain.Friendship;
import com.teamfiv5.fiv5.domain.FriendshipStatus;
import com.teamfiv5.fiv5.domain.User;
import com.teamfiv5.fiv5.dto.FriendDto;
import com.teamfiv5.fiv5.dto.UserDto;
import com.teamfiv5.fiv5.global.exception.CustomException;
import com.teamfiv5.fiv5.global.exception.code.ErrorCode;
import com.teamfiv5.fiv5.repository.FriendshipRepository;
import com.teamfiv5.fiv5.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class FriendService {

    private final UserRepository userRepository;
    private final FriendshipRepository friendshipRepository;

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public FriendDto.DiscoveryTokenResponse getBluetoothToken(Long myUserId) {
        User user = findUserById(myUserId);

        String token = user.getBluetoothToken();

        if (token == null) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }

        return new FriendDto.DiscoveryTokenResponse(token);
    }

    @Transactional(readOnly = true)
    public List<FriendDto.DiscoveredUserResponse> findUsersByTokens(Long myUserId, List<String> tokens) {
        if (tokens == null || tokens.isEmpty()) {
            return List.of();
        }

        List<User> users = userRepository.findByBluetoothTokenIn(tokens);

        Map<Long, Friendship> friendshipMap = friendshipRepository.findAllFriendshipsByUserId(myUserId)
                .stream()
                .collect(Collectors.toMap(
                        f -> f.getRequester().getId().equals(myUserId) ? f.getReceiver().getId() : f.getRequester().getId(),
                        Function.identity()
                ));

        return users.stream()
                .filter(user -> !user.getId().equals(myUserId))
                .map(user -> {
                    Friendship friendship = friendshipMap.get(user.getId());
                    FriendDto.FriendshipStatusInfo statusInfo = calculateFriendshipStatus(myUserId, friendship);
                    return FriendDto.DiscoveredUserResponse.of(user, statusInfo);
                })
                .collect(Collectors.toList());
    }

    private FriendDto.FriendshipStatusInfo calculateFriendshipStatus(Long myUserId, Friendship friendship) {
        if (friendship == null) {
            return FriendDto.FriendshipStatusInfo.NONE;
        }

        if (friendship.getStatus() == FriendshipStatus.FRIENDSHIP) {
            return FriendDto.FriendshipStatusInfo.FRIEND;
        }

        if (friendship.getRequester().getId().equals(myUserId)) {
            return FriendDto.FriendshipStatusInfo.PENDING_ME_TO_THEM;
        } else {
            return FriendDto.FriendshipStatusInfo.PENDING_THEM_TO_ME;
        }
    }

    @Transactional
    public void requestFriend(Long myUserId, Long targetUserId) {
        User target = findUserById(targetUserId);

        if (myUserId.equals(target.getId())) {
            throw new CustomException(ErrorCode.SELF_FRIEND_REQUEST);
        }

        User me = findUserById(myUserId);

        boolean alreadyExists = friendshipRepository.existsFriendshipBetween(myUserId, target.getId());

        if (alreadyExists) {
            throw new CustomException(ErrorCode.FRIEND_REQUEST_ALREADY_EXISTS);
        }

        Friendship friendship = Friendship.builder()
                .requester(me)
                .receiver(target)
                .status(FriendshipStatus.PENDING)
                .build();

        friendshipRepository.save(friendship);

        // TODO: 알림 생성 로직 추가
    }

    @Transactional
    public void acceptFriend(Long myUserId, Long requesterId) {
        Friendship friendship = friendshipRepository
                .findByRequesterIdAndReceiverIdAndStatus(requesterId, myUserId, FriendshipStatus.PENDING)
                .orElseThrow(() -> new CustomException(ErrorCode.FRIEND_REQUEST_NOT_FOUND));

        friendship.accept();
    }

    public List<UserDto.UserResponse> getReceivedFriendRequests(Long myUserId) {
        List<Friendship> pendingRequests = friendshipRepository.findByReceiverIdAndStatusWithRequester(
                myUserId,
                FriendshipStatus.PENDING
        );

        return pendingRequests.stream()
                .map(Friendship::getRequester)
                .map(UserDto.UserResponse::from)
                .collect(Collectors.toList());
    }

    public List<UserDto.UserResponse> getSentFriendRequests(Long myUserId) {
        List<Friendship> sentRequests = friendshipRepository.findByRequesterIdAndStatusWithReceiver(
                myUserId,
                FriendshipStatus.PENDING
        );

        return sentRequests.stream()
                .map(Friendship::getReceiver)
                .map(UserDto.UserResponse::from)
                .collect(Collectors.toList());
    }

    public List<UserDto.UserResponse> getMyFriends(Long myUserId) {
        List<Friendship> friendships = friendshipRepository.findAllFriendsWithUsers(
                myUserId,
                FriendshipStatus.FRIENDSHIP
        );

        return friendships.stream()
                .map(f -> {
                    if (f.getRequester().getId().equals(myUserId)) {
                        return f.getReceiver();
                    } else {
                        return f.getRequester();
                    }
                })
                .map(UserDto.UserResponse::from)
                .collect(Collectors.toList());
    }
}