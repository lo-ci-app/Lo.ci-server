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

import java.security.SecureRandom;
import java.util.Base64;
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
    private static final SecureRandom random = new SecureRandom();

    // 공용: 사용자 ID로 User 엔티티 조회
    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    // --- 1. 블루투스 친구 찾기 ---

    /**
     * (API 1 - 시작) 내 블루투스 토큰 발급/갱신
     */
    @Transactional
    public FriendDto.DiscoveryTokenResponse refreshBluetoothToken(Long myUserId) {
        User user = findUserById(myUserId);

        byte[] tokenBytes = new byte[6];
        random.nextBytes(tokenBytes);
        String newToken = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);

        user.updateBluetoothToken(newToken);
        return new FriendDto.DiscoveryTokenResponse(newToken);
    }

    /**
     * (API 1 - 중지) 내 블루투스 토큰 만료
     */
    @Transactional
    public void stopBluetoothToken(Long myUserId) {
        User user = findUserById(myUserId);
        user.updateBluetoothToken(null);
    }

    /**
     * (API 2) 토큰 목록으로 사용자 목록 및 친구 상태 조회
     */
    @Transactional(readOnly = true)
    public List<FriendDto.DiscoveredUserResponse> findUsersByTokens(Long myUserId, List<String> tokens) {
        if (tokens == null || tokens.isEmpty()) {
            return List.of();
        }

        // 1. 토큰으로 사용자 조회
        List<User> users = userRepository.findByBluetoothTokenIn(tokens);

        // 2. 내 모든 친구 관계(요청 포함)를 미리 조회 (N+1 문제 방지)
        Map<Long, Friendship> friendshipMap = friendshipRepository.findAllFriendshipsByUserId(myUserId)
                .stream()
                .collect(Collectors.toMap(
                        f -> f.getRequester().getId().equals(myUserId) ? f.getReceiver().getId() : f.getRequester().getId(),
                        Function.identity()
                ));

        // 3. (중요) 조회된 사용자와 내 친구 관계를 매핑
        return users.stream()
                .filter(user -> !user.getId().equals(myUserId))
                .map(user -> {
                    Friendship friendship = friendshipMap.get(user.getId());
                    FriendDto.FriendshipStatusInfo statusInfo = calculateFriendshipStatus(myUserId, friendship);
                    return FriendDto.DiscoveredUserResponse.of(user, statusInfo);
                })
                .collect(Collectors.toList());
    }

    // (로직) 친구 상태 계산
    private FriendDto.FriendshipStatusInfo calculateFriendshipStatus(Long myUserId, Friendship friendship) {
        if (friendship == null) {
            return FriendDto.FriendshipStatusInfo.NONE;
        }

        if (friendship.getStatus() == FriendshipStatus.FRIENDSHIP) {
            return FriendDto.FriendshipStatusInfo.FRIEND;
        }

        // PENDING 상태일 때, 내가 요청자인지(requester) 수신자인지(receiver) 확인
        if (friendship.getRequester().getId().equals(myUserId)) {
            return FriendDto.FriendshipStatusInfo.PENDING_ME_TO_THEM;
        } else {
            return FriendDto.FriendshipStatusInfo.PENDING_THEM_TO_ME;
        }
    }

    // --- 2. 친구 요청/수락 ---

    /**
     * (API 3) 친구 요청 (수정됨)
     */
    @Transactional
    public void requestFriend(Long myUserId, Long targetUserId) {
        // 1. ID로 상대방 조회 (findUserById 헬퍼 메서드 사용)
        User target = findUserById(targetUserId);

        // 2. 자신에게 요청하는지 확인 (ID 비교)
        if (myUserId.equals(target.getId())) {
            throw new CustomException(ErrorCode.SELF_FRIEND_REQUEST);
        }

        User me = findUserById(myUserId);

        // 3. (예외 처리) A->B, B->A 중복 요청 방지 (ID 비교)
        boolean alreadyExists = friendshipRepository.existsFriendshipBetween(myUserId, target.getId());

        if (alreadyExists) {
            throw new CustomException(ErrorCode.FRIEND_REQUEST_ALREADY_EXISTS);
        }

        // 5. 친구 요청 생성
        Friendship friendship = Friendship.builder()
                .requester(me)
                .receiver(target)
                .status(FriendshipStatus.PENDING)
                .build();

        friendshipRepository.save(friendship);

        // TODO: 알림 생성 로직 추가
    }

    /**
     * (API 4) 친구 수락
     */
    @Transactional
    public void acceptFriend(Long myUserId, Long requesterId) {
        // (참고) 친구 수락 API는 이미 알림/요청 목록을 받은 상태이므로 id(PK)를 사용해도 보안상 안전합니다.
        Friendship friendship = friendshipRepository
                .findByRequesterIdAndReceiverIdAndStatus(requesterId, myUserId, FriendshipStatus.PENDING)
                .orElseThrow(() -> new CustomException(ErrorCode.FRIEND_REQUEST_NOT_FOUND));

        friendship.accept();
    }

    /**
     * (신규 API 6) 내가 받은 친구 요청 목록 조회
     * * @param myUserId (로그인한 유저, 즉 '수신자(receiver)')
     * @return 나에게 요청을 보낸 사람들(requester)의 프로필 목록
     */
    public List<UserDto.UserResponse> getReceivedFriendRequests(Long myUserId) {
        // 1. 내가 '수신자(receiver)'이고 상태가 'PENDING'인 모든 요청을 조회
        List<Friendship> pendingRequests = friendshipRepository.findByReceiverIdAndStatusWithRequester(
                myUserId,
                FriendshipStatus.PENDING
        );

        // 2. Friendship 목록에서 '요청자(requester)'의 User 정보만 추출
        return pendingRequests.stream()
                .map(Friendship::getRequester) // (Friendship -> User)
                .map(UserDto.UserResponse::from) // (User -> UserDto.UserResponse)
                .collect(Collectors.toList());
    }

    /**
     * (신규 API 7) 내가 보낸 친구 요청 목록 조회
     * * @param myUserId (로그인한 유저, 즉 '요청자(requester)')
     * @return 내가 요청을 보낸 사람들(receiver)의 프로필 목록
     */
    public List<UserDto.UserResponse> getSentFriendRequests(Long myUserId) {
        // 1. 내가 '요청자(requester)'이고 상태가 'PENDING'인 모든 요청을 조회
        List<Friendship> sentRequests = friendshipRepository.findByRequesterIdAndStatusWithReceiver(
                myUserId,
                FriendshipStatus.PENDING
        );

        // 2. Friendship 목록에서 '수신자(receiver)'의 User 정보만 추출
        return sentRequests.stream()
                .map(Friendship::getReceiver) // (Friendship -> User)
                .map(UserDto.UserResponse::from) // (User -> UserDto.UserResponse)
                .collect(Collectors.toList());
    }

    /**
     * (신규 API 8) 내 친구 목록 조회
     * * @param myUserId
     * @return 나와 'FRIENDSHIP' 상태인 사람들의 프로필 목록
     */
    public List<UserDto.UserResponse> getMyFriends(Long myUserId) {
        // 1. 내가 포함된(A든 B든) 'FRIENDSHIP' 상태인 모든 관계 조회
        List<Friendship> friendships = friendshipRepository.findAllFriendsWithUsers(
                myUserId,
                FriendshipStatus.FRIENDSHIP
        );

        // 2. 상대방의 User 정보만 추출
        return friendships.stream()
                .map(f -> {
                    // 내가 요청자(requester)면 상대방(receiver)을 반환
                    if (f.getRequester().getId().equals(myUserId)) {
                        return f.getReceiver();
                    }
                    // 내가 수신자(receiver)면 상대방(requester)을 반환
                    else {
                        return f.getRequester();
                    }
                })
                .map(UserDto.UserResponse::from)
                .collect(Collectors.toList());
    }
}