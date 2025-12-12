package com.teamloci.loci.domain.friend;

import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.teamloci.loci.domain.intimacy.entity.FriendshipIntimacy;
import com.teamloci.loci.domain.intimacy.entity.IntimacyType;
import com.teamloci.loci.domain.intimacy.service.IntimacyService;
import com.teamloci.loci.domain.notification.NotificationService;
import com.teamloci.loci.domain.notification.NotificationType;
import com.teamloci.loci.domain.user.User;
import com.teamloci.loci.domain.user.UserDto;
import com.teamloci.loci.domain.user.UserRepository;
import com.teamloci.loci.domain.user.UserStatus;
import com.teamloci.loci.domain.user.UserActivityService;
import com.teamloci.loci.global.error.CustomException;
import com.teamloci.loci.global.error.ErrorCode;
import com.teamloci.loci.global.util.AesUtil;
import com.teamloci.loci.global.util.RelationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class FriendService {

    private static final int MAX_FRIEND_LIMIT = 20;

    private final UserRepository userRepository;
    private final FriendshipRepository friendshipRepository;
    private final NotificationService notificationService;
    private final UserContactRepository userContactRepository;
    private final AesUtil aesUtil;
    private final UserActivityService userActivityService;
    private final IntimacyService intimacyService;

    private record ContactInfo(String name, String e164PhoneNumber) {}

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    @Transactional
    public List<UserDto.UserResponse> matchFriends(Long myUserId, List<FriendDto.ContactRequest> contacts) {
        User me = findUserById(myUserId);
        String defaultRegion = StringUtils.hasText(me.getCountryCode()) ? me.getCountryCode() : "KR";
        PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();

        Map<String, ContactInfo> inputContactsMap = new HashMap<>();

        for (FriendDto.ContactRequest c : contacts) {
            try {
                var parsed = phoneUtil.parse(c.getPhoneNumber(), defaultRegion);
                String e164 = phoneUtil.format(parsed, PhoneNumberUtil.PhoneNumberFormat.E164);
                String name = c.getName() != null ? c.getName() : "";

                String hash = aesUtil.hash(e164);
                inputContactsMap.put(hash, new ContactInfo(name, e164));
            } catch (Exception e) {
            }
        }

        List<String> allInputHashes = new ArrayList<>(inputContactsMap.keySet());

        List<UserContact> existingContacts = userContactRepository.findByUserId(myUserId);
        List<UserContact> toDelete = new ArrayList<>();

        for (UserContact existing : existingContacts) {
            String existingHash = existing.getPhoneSearchHash();

            if (existingHash == null) {
                try {
                    String decrypted = aesUtil.decrypt(existing.getPhoneNumber());
                    existingHash = aesUtil.hash(decrypted);
                    existing.updatePhoneSearchHash(existingHash);
                } catch (Exception e) {
                log.error("연락처 마이그레이션 실패 (ID: {}): 데이터 손상을 방지하기 위해 건너뜁니다. {}", existing.getId(), e.getMessage());
                continue;
                }
            }

            if (inputContactsMap.containsKey(existingHash)) {
                ContactInfo inputInfo = inputContactsMap.get(existingHash);
                if (!inputInfo.name().equals(existing.getName())) {
                    existing.updateName(inputInfo.name());
                }
                inputContactsMap.remove(existingHash);
            } else {
                toDelete.add(existing);
            }
        }

        userContactRepository.deleteAll(toDelete);

        List<UserContact> toSave = new ArrayList<>();
        for (Map.Entry<String, ContactInfo> entry : inputContactsMap.entrySet()) {
            String hash = entry.getKey();
            ContactInfo info = entry.getValue();

            String encryptedPhone = aesUtil.encrypt(info.e164PhoneNumber());

            toSave.add(UserContact.builder()
                    .user(me)
                    .phoneNumber(encryptedPhone)
                    .phoneSearchHash(hash)
                    .name(info.name())
                    .build());
        }
        userContactRepository.saveAll(toSave);

        if (allInputHashes.isEmpty()) return List.of();

        List<User> matchedUsers = userRepository.findByPhoneSearchHashIn(allInputHashes).stream()
                .filter(user -> !user.getId().equals(myUserId))
                .filter(user -> user.getStatus() == UserStatus.ACTIVE)
                .collect(Collectors.toList());

        return buildUserResponses(myUserId, matchedUsers);
    }

    public List<UserDto.UserResponse> getSyncedContacts(Long userId) {
        List<String> phoneHashes = userContactRepository.findPhoneSearchHashesByUserId(userId);

        if (phoneHashes.isEmpty()) {
            return List.of();
        }

        List<User> matchedUsers = userRepository.findByPhoneSearchHashIn(phoneHashes).stream()
                .filter(u -> !u.getId().equals(userId))
                .filter(u -> u.getStatus() == UserStatus.ACTIVE)
                .collect(Collectors.toList());

        return buildUserResponses(userId, matchedUsers);
    }

    @Transactional
    public void acceptFriendRequest(Long myUserId, Long requesterId) {
        User me = userRepository.findByIdWithLock(myUserId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        User requester = userRepository.findByIdWithLock(requesterId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Friendship friendship = friendshipRepository.findFriendshipBetween(myUserId, requesterId)
                .orElseThrow(() -> new CustomException(ErrorCode.FRIEND_REQUEST_NOT_FOUND));

        if (!friendship.getReceiver().getId().equals(myUserId)) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        if (friendship.getStatus() == FriendshipStatus.FRIENDSHIP) {
            throw new CustomException(ErrorCode.FRIEND_REQUEST_ALREADY_EXISTS);
        }

        long myRealFriendCount = friendshipRepository.countFriends(myUserId);
        long requesterRealFriendCount = friendshipRepository.countFriends(requesterId);

        if (myRealFriendCount >= MAX_FRIEND_LIMIT) {
            throw new CustomException(ErrorCode.FRIEND_LIMIT_EXCEEDED);
        }
        if (requesterRealFriendCount >= MAX_FRIEND_LIMIT) {
            throw new CustomException(ErrorCode.TARGET_FRIEND_LIMIT_EXCEEDED);
        }

        friendship.accept();

        userRepository.increaseFriendCount(myUserId);
        userRepository.increaseFriendCount(requesterId);

        intimacyService.accumulatePoint(myUserId, requesterId, IntimacyType.FRIEND_MADE, null);

        notificationService.send(
                requester,
                NotificationType.FRIEND_ACCEPTED,
                "친구 수락",
                me.getNickname() + "님과 친구가 되었습니다!",
                me.getId(),
                me.getProfileUrl()
        );
    }

    @Transactional
    public void sendFriendRequest(Long myUserId, Long targetUserId) {
        if (myUserId.equals(targetUserId)) throw new CustomException(ErrorCode.SELF_FRIEND_REQUEST);

        User me = userRepository.findByIdWithLock(myUserId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        User target = userRepository.findByIdWithLock(targetUserId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Optional<Friendship> existing = friendshipRepository.findFriendshipBetween(myUserId, targetUserId);

        long myRealFriendCount = friendshipRepository.countFriends(myUserId);
        long targetRealFriendCount = friendshipRepository.countFriends(targetUserId);

        if (existing.isPresent()) {
            Friendship f = existing.get();
            if (f.getStatus() == FriendshipStatus.FRIENDSHIP) {
                throw new CustomException(ErrorCode.FRIEND_REQUEST_ALREADY_EXISTS);
            }
            if (f.getRequester().getId().equals(myUserId)) {
                throw new CustomException(ErrorCode.FRIEND_REQUEST_ALREADY_EXISTS);
            } else {
                if (myRealFriendCount >= MAX_FRIEND_LIMIT) {
                    throw new CustomException(ErrorCode.FRIEND_LIMIT_EXCEEDED);
                }
                if (targetRealFriendCount >= MAX_FRIEND_LIMIT) {
                    throw new CustomException(ErrorCode.TARGET_FRIEND_LIMIT_EXCEEDED);
                }

                f.accept();
                userRepository.increaseFriendCount(myUserId);
                userRepository.increaseFriendCount(targetUserId);
                return;
            }
        }

        if (myRealFriendCount >= MAX_FRIEND_LIMIT) {
            throw new CustomException(ErrorCode.FRIEND_LIMIT_EXCEEDED);
        }
        if (targetRealFriendCount >= MAX_FRIEND_LIMIT) {
            throw new CustomException(ErrorCode.TARGET_FRIEND_LIMIT_EXCEEDED);
        }

        Friendship friendship = Friendship.builder()
                .requester(me)
                .receiver(target)
                .status(FriendshipStatus.PENDING)
                .build();

        friendshipRepository.save(friendship);

        notificationService.send(
                target,
                NotificationType.FRIEND_REQUEST,
                "친구 요청",
                me.getNickname() + "님이 친구 요청을 보냈습니다.",
                me.getId(),
                me.getProfileUrl()
        );
    }

    @Transactional
    public void deleteFriendship(Long myUserId, Long targetUserId) {
        Friendship friendship = friendshipRepository.findFriendshipBetween(myUserId, targetUserId)
                .orElseThrow(() -> new CustomException(ErrorCode.FRIEND_REQUEST_NOT_FOUND));

        if (friendship.getStatus() == FriendshipStatus.FRIENDSHIP) {
            userRepository.decreaseFriendCount(myUserId);
            userRepository.decreaseFriendCount(targetUserId);
        }

        friendshipRepository.delete(friendship);
    }

    public List<UserDto.UserResponse> getMyFriends(Long myUserId) {
        List<User> friends = friendshipRepository.findAllFriendsWithUsers(myUserId)
                .stream()
                .map(f -> f.getRequester().getId().equals(myUserId) ? f.getReceiver() : f.getRequester())
                .collect(Collectors.toList());

        return buildUserResponses(myUserId, friends);
    }

    public List<UserDto.UserResponse> getReceivedRequests(Long myUserId) {
        List<User> requesters = friendshipRepository.findReceivedRequests(myUserId).stream()
                .map(Friendship::getRequester)
                .collect(Collectors.toList());

        return buildUserResponses(myUserId, requesters);
    }

    public List<UserDto.UserResponse> getSentRequests(Long myUserId) {
        List<User> receivers = friendshipRepository.findSentRequests(myUserId).stream()
                .map(Friendship::getReceiver)
                .collect(Collectors.toList());

        return buildUserResponses(myUserId, receivers);
    }

    public UserDto.UserSearchResponse searchUsers(Long myUserId, String keyword, Long cursorId, int size) {
        if (keyword == null || keyword.isBlank()) {
            return UserDto.UserSearchResponse.builder().users(List.of()).hasNext(false).build();
        }

        long currentCursor = (cursorId == null) ? Long.MAX_VALUE : cursorId;
        Pageable pageable = PageRequest.of(0, size + 1);
        List<User> foundUsers = userRepository.searchByKeywordWithCursor(keyword, currentCursor, pageable);

        boolean hasNext = false;
        if (foundUsers.size() > size) {
            hasNext = true;
            foundUsers.remove(size);
        }
        Long nextCursor = foundUsers.isEmpty() ? null : foundUsers.get(foundUsers.size() - 1).getId();

        List<UserDto.UserResponse> userDtos = buildUserResponses(myUserId, foundUsers);

        return UserDto.UserSearchResponse.builder()
                .users(userDtos)
                .hasNext(hasNext)
                .nextCursor(nextCursor)
                .build();
    }

    private List<UserDto.UserResponse> buildUserResponses(Long myUserId, List<User> users) {
        if (users.isEmpty()) return List.of();

        List<Long> userIds = users.stream().map(User::getId).toList();

        List<Friendship> friendships = friendshipRepository.findAllRelationsBetween(myUserId, userIds);
        Map<Long, Friendship> friendshipMap = friendships.stream()
                .collect(Collectors.toMap(
                        f -> f.getRequester().getId().equals(myUserId) ? f.getReceiver().getId() : f.getRequester().getId(),
                        f -> f
                ));

        Map<Long, UserActivityService.UserStats> statsMap = userActivityService.getUserStatsMap(userIds);
        Map<Long, FriendshipIntimacy> myIntimacyMap = intimacyService.getIntimacyMap(myUserId, userIds);

        return users.stream().map(user -> {
            var stats = statsMap.getOrDefault(user.getId(), new UserActivityService.UserStats(0,0,0,0,0));

            String status = "NONE";
            if (user.getId().equals(myUserId)) {
                status = "SELF";
            } else {
                Friendship f = friendshipMap.get(user.getId());
                status = RelationUtil.resolveStatus(f, myUserId);
            }

            UserDto.UserResponse response = UserDto.UserResponse.of(
                    user, status, stats.friendCount(), stats.postCount(), stats.streakCount(), stats.visitedPlaceCount()
            );

            response.applyIntimacyInfo(myIntimacyMap.get(user.getId()), stats.totalIntimacyLevel());

            return response;
        }).collect(Collectors.toList());
    }
}