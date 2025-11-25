package com.teamloci.loci.service;

import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.teamloci.loci.domain.*;
import com.teamloci.loci.dto.FriendDto;
import com.teamloci.loci.dto.UserDto;
import com.teamloci.loci.global.exception.CustomException;
import com.teamloci.loci.global.exception.code.ErrorCode;
import com.teamloci.loci.global.util.AesUtil;
import com.teamloci.loci.repository.FriendshipRepository;
import com.teamloci.loci.repository.UserContactRepository;
import com.teamloci.loci.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
    private final UserContactRepository userContactRepository;
    private final AesUtil aesUtil;

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    @Transactional
    public List<UserDto.UserResponse> matchFriends(Long myUserId, List<FriendDto.ContactRequest> contacts) {
        User me = findUserById(myUserId);
        String defaultRegion = StringUtils.hasText(me.getCountryCode()) ? me.getCountryCode() : "KR";
        PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();

        Map<String, String> newContactsMap = contacts.stream()
                .map(c -> {
                    try {
                        var parsed = phoneUtil.parse(c.getPhoneNumber(), defaultRegion);
                        String e164 = phoneUtil.format(parsed, PhoneNumberUtil.PhoneNumberFormat.E164);
                        String name = c.getName() != null ? c.getName() : "";
                        return Map.entry(e164, name);
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (v1, v2) -> v1));

        List<String> allPhoneNumbers = new ArrayList<>(newContactsMap.keySet());

        List<UserContact> existingContacts = userContactRepository.findByUserId(myUserId);

        List<UserContact> toDelete = existingContacts.stream()
                .filter(ec -> !newContactsMap.containsKey(ec.getPhoneNumber()))
                .collect(Collectors.toList());
        userContactRepository.deleteAll(toDelete);

        for (UserContact existing : existingContacts) {
            if (newContactsMap.containsKey(existing.getPhoneNumber())) {
                String newName = newContactsMap.get(existing.getPhoneNumber());

                if (!newName.equals(existing.getName())) {
                    existing.updateName(newName);
                }
                newContactsMap.remove(existing.getPhoneNumber());
            }
        }

        List<UserContact> toSave = newContactsMap.entrySet().stream()
                .map(entry -> UserContact.builder()
                        .user(me)
                        .phoneNumber(entry.getKey())
                        .name(entry.getValue())
                        .build())
                .collect(Collectors.toList());
        userContactRepository.saveAll(toSave);

        List<String> allPhoneHashes = allPhoneNumbers.stream()
                .map(aesUtil::hash)
                .collect(Collectors.toList());

        if (allPhoneHashes.isEmpty()) return List.of();

        List<User> matchedUsers = userRepository.findByPhoneSearchHashIn(allPhoneHashes).stream()
                .filter(user -> !user.getId().equals(myUserId))
                .filter(user -> user.getStatus() == UserStatus.ACTIVE)
                .collect(Collectors.toList());

        List<Long> targetIds = matchedUsers.stream().map(User::getId).toList();
        Map<Long, Friendship> friendshipMap = getFriendshipMap(myUserId, targetIds);

        return matchedUsers.stream()
                .map(user -> {
                    String status = resolveRelationStatus(friendshipMap.get(user.getId()), myUserId);
                    return UserDto.UserResponse.of(user, status, 0L, 0L);
                })
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
                .map(user -> UserDto.UserResponse.of(user, "FRIEND", 0L, 0L))
                .collect(Collectors.toList());
    }

    public List<UserDto.UserResponse> getReceivedRequests(Long myUserId) {
        return friendshipRepository.findReceivedRequests(myUserId).stream()
                .map(Friendship::getRequester)
                .map(user -> UserDto.UserResponse.of(user, "PENDING_RECEIVED", 0L, 0L))
                .collect(Collectors.toList());
    }

    public List<UserDto.UserResponse> getSentRequests(Long myUserId) {
        return friendshipRepository.findSentRequests(myUserId).stream()
                .map(Friendship::getReceiver)
                .map(user -> UserDto.UserResponse.of(user, "PENDING_SENT", 0L, 0L))
                .collect(Collectors.toList());
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

        List<Long> targetIds = foundUsers.stream().map(User::getId).collect(Collectors.toList());
        Map<Long, Friendship> friendshipMap = getFriendshipMap(myUserId, targetIds);

        List<UserDto.UserResponse> userDtos = foundUsers.stream()
                .map(user -> {
                    if (user.getId().equals(myUserId)) {
                        return UserDto.UserResponse.of(user, "SELF", 0L, 0L);
                    }
                    String status = resolveRelationStatus(friendshipMap.get(user.getId()), myUserId);
                    return UserDto.UserResponse.of(user, status, 0L, 0L);
                })
                .collect(Collectors.toList());

        return UserDto.UserSearchResponse.builder()
                .users(userDtos)
                .hasNext(hasNext)
                .nextCursor(nextCursor)
                .build();
    }

    private Map<Long, Friendship> getFriendshipMap(Long myUserId, List<Long> targetIds) {
        List<Friendship> friendships = friendshipRepository.findAllRelationsBetween(myUserId, targetIds);
        return friendships.stream()
                .collect(Collectors.toMap(
                        f -> f.getRequester().getId().equals(myUserId) ? f.getReceiver().getId() : f.getRequester().getId(),
                        f -> f
                ));
    }

    private String resolveRelationStatus(Friendship friendship, Long myUserId) {
        if (friendship == null) return "NONE";
        if (friendship.getStatus() == FriendshipStatus.FRIENDSHIP) return "FRIEND";

        if (friendship.getRequester().getId().equals(myUserId)) {
            return "PENDING_SENT";
        } else {
            return "PENDING_RECEIVED";
        }
    }
}