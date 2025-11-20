package com.teamloci.loci.service;

import com.google.cloud.firestore.Firestore;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.messaging.FirebaseMessaging;
import com.teamloci.loci.domain.Friendship;
import com.teamloci.loci.domain.FriendshipStatus;
import com.teamloci.loci.domain.User;
import com.teamloci.loci.dto.FriendDto;
import com.teamloci.loci.dto.UserDto;
import com.teamloci.loci.global.exception.CustomException;
import com.teamloci.loci.global.exception.code.ErrorCode;
import com.teamloci.loci.global.util.AesUtil;
import com.teamloci.loci.repository.FriendshipRepository;
import com.teamloci.loci.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.S3Client;

import java.util.HexFormat;
import java.util.List;
import java.security.SecureRandom;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
class FriendServiceIntegrationTest {

    @Autowired
    private FriendService friendService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FriendshipRepository friendshipRepository;

    @Autowired
    private AesUtil aesUtil;

    @MockBean
    private Firestore firestore;

    @MockBean
    private FirebaseAuth firebaseAuth;

    @MockBean
    private FirebaseMessaging firebaseMessaging;

    @MockBean
    private NotificationService notificationService;

    @MockBean
    private S3Client s3Client;

    private User userA;
    private User userB;
    private User userC;

    @BeforeEach
    void setUp() {
        // 기본 유저 세팅 (전화번호 없는 상태)
        userA = User.builder()
                .email("a@test.com")
                .nickname("UserA")
                .provider("apple")
                .providerId("provider_A")
                .countryCode("KR")
                .build();

        userB = User.builder()
                .email("b@test.com")
                .nickname("UserB")
                .provider("apple")
                .providerId("provider_B")
                .build();

        userC = User.builder()
                .email("c@test.com")
                .nickname("UserC")
                .provider("apple")
                .providerId("provider_C")
                .build();

        userA.updateBluetoothToken(generateTestToken());
        userB.updateBluetoothToken(generateTestToken());
        userC.updateBluetoothToken(generateTestToken());

        userRepository.saveAll(List.of(userA, userB, userC));
    }

    private String generateTestToken() {
        SecureRandom random = new SecureRandom();
        HexFormat hexFormat = HexFormat.of();
        byte[] tokenBytes = new byte[4];
        String newToken;
        do {
            random.nextBytes(tokenBytes);
            newToken = hexFormat.formatHex(tokenBytes);
        } while (userRepository.existsByBluetoothToken(newToken));
        return newToken;
    }

    @Test
    @DisplayName("고정 토큰 조회 시 DB에 저장된 토큰이 반환된다")
    void getBluetoothToken_Success() {
        String fixedTokenA = userA.getBluetoothToken();
        assertThat(fixedTokenA).isNotNull();

        String tokenA = friendService.getBluetoothToken(userA.getId()).getBluetoothToken();

        assertThat(tokenA).isEqualTo(fixedTokenA);
    }

    @Test
    @DisplayName("토큰 목록 조회 시 ID, 닉네임, 상태(NONE)가 반환된다")
    void findUsersByTokens_Status_NONE() {
        String tokenB = userB.getBluetoothToken();
        String tokenC = userC.getBluetoothToken();

        List<FriendDto.DiscoveredUserResponse> result = friendService.findUsersByTokens(
                userA.getId(), List.of(tokenB, tokenC, "fake-token")
        );

        assertThat(result).hasSize(2);
        assertThat(result)
                .extracting(FriendDto.DiscoveredUserResponse::getNickname)
                .containsExactlyInAnyOrder("UserB", "UserC");
    }

    @Test
    @DisplayName("성공: 주소록의 010 번호로 가입된 친구를 찾는다")
    void matchFriends_Success() {
        String rawPhoneNumber = "010-1234-5678";
        String e164PhoneNumber = "+821012345678";
        String searchHash = aesUtil.hash(e164PhoneNumber);

        User userD = User.builder()
                .email("d@test.com")
                .nickname("PhoneFriend")
                .provider("phone")
                .providerId("firebase_uid_d")
                .phoneSearchHash(searchHash)
                .countryCode("KR")
                .build();
        userRepository.save(userD);

        List<String> myContacts = List.of(rawPhoneNumber, "010-9999-9999");

        List<UserDto.UserResponse> matched = friendService.matchFriends(userA.getId(), myContacts);

        assertThat(matched).hasSize(1);
        assertThat(matched.get(0).getNickname()).isEqualTo("PhoneFriend");
        assertThat(matched.get(0).getId()).isEqualTo(userD.getId());
    }

    private void createAndSaveFriends(User user, int count) {
        List<User> dummyFriends = IntStream.range(1, count + 1)
                .mapToObj(i -> User.builder()
                        .email("friend" + i + "@test.com")
                        .nickname("F" + i + "_" + user.getNickname())
                        .provider("apple")
                        .providerId("f" + i + "_" + user.getNickname())
                        .build())
                .peek(u -> u.updateBluetoothToken(generateTestToken()))
                .toList();
        userRepository.saveAll(dummyFriends);

        dummyFriends.forEach(friend ->
                friendshipRepository.save(Friendship.builder()
                        .requester(user)
                        .receiver(friend)
                        .status(FriendshipStatus.FRIENDSHIP)
                        .build())
        );
    }

    @Test
    @DisplayName("성공: A가 B의 ID로 친구 요청을 보낸다")
    void requestFriend_Success() {
        Long idB = userB.getId();

        friendService.requestFriend(userA.getId(), idB);

        List<Friendship> friendships = friendshipRepository.findAll();
        assertThat(friendships).hasSize(1);
        Friendship friendship = friendships.get(0);
        assertThat(friendship.getRequester().getId()).isEqualTo(userA.getId());
        assertThat(friendship.getReceiver().getId()).isEqualTo(userB.getId());
        assertThat(friendship.getStatus()).isEqualTo(FriendshipStatus.PENDING);
    }

    @Test
    @DisplayName("실패: A가 자신의 ID로 친구 요청 (SELF_FRIEND_REQUEST)")
    void requestFriend_SelfRequest() {
        Long idA = userA.getId();

        assertThatThrownBy(() -> friendService.requestFriend(userA.getId(), idA))
                .isInstanceOf(CustomException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.SELF_FRIEND_REQUEST);
    }

    @Test
    @DisplayName("실패: A가 이미 친구인 B에게 다시 요청 (FRIEND_REQUEST_ALREADY_EXISTS)")
    void requestFriend_AlreadyExists() {
        friendshipRepository.save(Friendship.builder()
                .requester(userA)
                .receiver(userB)
                .status(FriendshipStatus.FRIENDSHIP)
                .build());

        Long idB = userB.getId();

        assertThatThrownBy(() -> friendService.requestFriend(userA.getId(), idB))
                .isInstanceOf(CustomException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.FRIEND_REQUEST_ALREADY_EXISTS);
    }

    @Test
    @DisplayName("성공: B가 A의 친구 요청을 수락한다")
    void acceptFriend_Success() {
        friendshipRepository.save(Friendship.builder()
                .requester(userA)
                .receiver(userB)
                .status(FriendshipStatus.PENDING)
                .build());

        friendService.acceptFriend(userB.getId(), userA.getId());

        List<Friendship> friendships = friendshipRepository.findAll();
        assertThat(friendships).hasSize(1);
        Friendship friendship = friendships.get(0);
        assertThat(friendship.getStatus()).isEqualTo(FriendshipStatus.FRIENDSHIP);
    }

    @Test
    @DisplayName("실패: 존재하지 않는 친구 요청 수락 (FRIEND_REQUEST_NOT_FOUND)")
    void acceptFriend_NotFound() {
        assertThatThrownBy(() -> friendService.acceptFriend(userB.getId(), userA.getId()))
                .isInstanceOf(CustomException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.FRIEND_REQUEST_NOT_FOUND);
    }

    @Test
    @DisplayName("실패: '내'가 친구 수 20명 제한에 도달 (FRIEND_LIMIT_EXCEEDED)")
    void acceptFriend_Fail_MyLimitExceeded() {
        createAndSaveFriends(userA, 20);

        friendshipRepository.save(Friendship.builder()
                .requester(userB)
                .receiver(userA)
                .status(FriendshipStatus.PENDING)
                .build());

        assertThat(friendshipRepository.countByUserIdAndStatus(userA.getId(), FriendshipStatus.FRIENDSHIP)).isEqualTo(20);

        assertThatThrownBy(() -> friendService.acceptFriend(userA.getId(), userB.getId()))
                .isInstanceOf(CustomException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.FRIEND_LIMIT_EXCEEDED);
    }

    @Test
    @DisplayName("실패: '상대방'이 친구 수 20명 제한에 도달 (TARGET_FRIEND_LIMIT_EXCEEDED)")
    void acceptFriend_Fail_TargetLimitExceeded() {
        createAndSaveFriends(userB, 20);

        friendshipRepository.save(Friendship.builder()
                .requester(userB)
                .receiver(userA)
                .status(FriendshipStatus.PENDING)
                .build());

        assertThat(friendshipRepository.countByUserIdAndStatus(userB.getId(), FriendshipStatus.FRIENDSHIP)).isEqualTo(20);
        assertThat(friendshipRepository.countByUserIdAndStatus(userA.getId(), FriendshipStatus.FRIENDSHIP)).isZero();

        assertThatThrownBy(() -> friendService.acceptFriend(userA.getId(), userB.getId()))
                .isInstanceOf(CustomException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.TARGET_FRIEND_LIMIT_EXCEEDED);
    }
}