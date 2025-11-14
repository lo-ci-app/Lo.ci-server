package com.teamfiv5.fiv5.service;

import com.teamfiv5.fiv5.domain.Friendship;
import com.teamfiv5.fiv5.domain.FriendshipStatus;
import com.teamfiv5.fiv5.domain.User;
import com.teamfiv5.fiv5.dto.FriendDto;
import com.teamfiv5.fiv5.global.exception.CustomException;
import com.teamfiv5.fiv5.global.exception.code.ErrorCode;
import com.teamfiv5.fiv5.repository.FriendshipRepository;
import com.teamfiv5.fiv5.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.HexFormat; // ◀◀◀ [추가]
import java.util.List;
import java.security.SecureRandom; // ◀◀◀ [추가]

import static org.assertj.core.api.Assertions.*;

@SpringBootTest // H2 DB 및 모든 빈(Service, Repository)을 로드
@Transactional  // 각 테스트 후 DB 자동 롤백
class FriendServiceIntegrationTest {

    @Autowired
    private FriendService friendService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FriendshipRepository friendshipRepository;

    // ◀◀◀ [추가] 테스트용 토큰 생성을 위해 AuthSvc 로직 일부 사용
    @Autowired
    private AuthService authService;

    private User userA;
    private User userB;
    private User userC;

    @BeforeEach
    void setUp() {
        // 테스트 실행 전, H2 DB에 테스트용 유저 3명 생성
        userA = User.builder().nickname("UserA").provider("apple").providerId("provider_A").build();
        userB = User.builder().nickname("UserB").provider("apple").providerId("provider_B").build();
        userC = User.builder().nickname("UserC").provider("apple").providerId("provider_C").build();

        // ◀◀◀ [수정] 고정 토큰을 미리 발급 (AuthService 로직 모방)
        // (실제로는 AuthService.loginWithApple()이 담당)
        userA.updateBluetoothToken(generateTestToken());
        userB.updateBluetoothToken(generateTestToken());
        userC.updateBluetoothToken(generateTestToken());

        userRepository.saveAll(List.of(userA, userB, userC));
    }

    // 테스트용 고유 토큰 생성기
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

    // --- API 1 (Token) ---
    @Test
    @DisplayName("고정 토큰 조회 시 DB에 저장된 토큰이 반환된다") // ◀◀◀ [수정]
    void getBluetoothToken_Success() { // ◀◀◀ [수정]
        // given
        String fixedTokenA = userA.getBluetoothToken();
        assertThat(fixedTokenA).isNotNull(); // setup에서 잘 저장되었는지 확인

        // when
        // 토큰 조회 API (구. refreshBluetoothToken) 호출
        String tokenA = friendService.getBluetoothToken(userA.getId()).getBluetoothToken(); // ◀◀◀ [수정]

        // then
        assertThat(tokenA).isEqualTo(fixedTokenA); // 저장된 토큰과 일치해야 함

        // ◀◀◀ [추가] 여러 번 호출해도 같은 토큰이 나와야 함 (고정)
        String tokenA_again = friendService.getBluetoothToken(userA.getId()).getBluetoothToken();
        assertThat(tokenA_again).isEqualTo(fixedTokenA);
    }

    // --- API 2 (Discovery) ---
    @Test
    @DisplayName("토큰 목록 조회 시 ID, 닉네임, 상태(NONE)가 반환된다") // ◀◀◀ [수정]
    void findUsersByTokens_Status_NONE() {
        // given
        String tokenB = userB.getBluetoothToken(); // ◀◀◀ [수정]
        String tokenC = userC.getBluetoothToken(); // ◀◀◀ [수정]

        // when
        // A가 B와 C를 스캔함
        List<FriendDto.DiscoveredUserResponse> result = friendService.findUsersByTokens(
                userA.getId(), List.of(tokenB, tokenC, "fake-token")
        );

        // then
        assertThat(result).hasSize(2);

        // 닉네임 검사
        assertThat(result)
                .extracting(FriendDto.DiscoveredUserResponse::getNickname)
                .containsExactlyInAnyOrder("UserB", "UserC");

        // ◀◀◀ [추가] ID 검사 (리팩토링 핵심)
        assertThat(result)
                .extracting(FriendDto.DiscoveredUserResponse::getId)
                .containsExactlyInAnyOrder(userB.getId(), userC.getId());

        // 상태 검사
        assertThat(result)
                .extracting(FriendDto.DiscoveredUserResponse::getFriendshipStatus)
                .containsOnly(FriendDto.FriendshipStatusInfo.NONE);
    }

    // --- API 3 (Request) ---
    @Test
    @DisplayName("성공: A가 B의 ID로 친구 요청을 보낸다") // ◀◀◀ [수정]
    void requestFriend_Success() {
        // given
        Long idB = userB.getId(); // ◀◀◀ [수정] (토큰 대신 ID 사용)

        // when
        friendService.requestFriend(userA.getId(), idB); // ◀◀◀ [수정]

        // then
        Friendship friendship = friendshipRepository.findAll().get(0);
        assertThat(friendship.getRequester().getId()).isEqualTo(userA.getId());
        assertThat(friendship.getReceiver().getId()).isEqualTo(userB.getId());
        assertThat(friendship.getStatus()).isEqualTo(FriendshipStatus.PENDING);

        // ◀◀◀ [수정] B의 토큰이 만료(null)되지 *않았는지* 확인 (고정 토큰)
        User updatedUserB = userRepository.findById(userB.getId()).get();
        assertThat(updatedUserB.getBluetoothToken()).isNotNull();
        assertThat(updatedUserB.getBluetoothToken()).isEqualTo(userB.getBluetoothToken());
    }

    @Test
    @DisplayName("실패: A가 자신의 ID로 친구 요청 (SELF_FRIEND_REQUEST)") // ◀◀◀ [수정]
    void requestFriend_SelfRequest() {
        // given
        Long idA = userA.getId(); // ◀◀◀ [수정]

        // when & then
        assertThatThrownBy(() -> friendService.requestFriend(userA.getId(), idA)) // ◀◀◀ [수정]
                .isInstanceOf(CustomException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.SELF_FRIEND_REQUEST);
    }

    @Test
    @DisplayName("실패: A가 이미 친구인 B에게 다시 요청 (FRIEND_REQUEST_ALREADY_EXISTS)") // ◀◀◀ [수정]
    void requestFriend_AlreadyExists() {
        // given
        // 1. A와 B가 이미 친구 상태
        friendshipRepository.save(Friendship.builder()
                .requester(userA)
                .receiver(userB)
                .status(FriendshipStatus.FRIENDSHIP)
                .build());

        // 2. B의 ID
        Long idB = userB.getId(); // ◀◀◀ [수정]

        // when & then
        // 3. A가 B의 ID로 다시 요청 시 예외 발생
        assertThatThrownBy(() -> friendService.requestFriend(userA.getId(), idB)) // ◀◀◀ [수정]
                .isInstanceOf(CustomException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.FRIEND_REQUEST_ALREADY_EXISTS);
    }

    // --- API 4 (Accept) ---
    @Test
    @DisplayName("성공: B가 A의 친구 요청을 수락한다")
    void acceptFriend_Success() {
        // given
        // 1. A가 B에게 친구 요청을 보낸 상태 (PENDING)
        friendshipRepository.save(Friendship.builder()
                .requester(userA)
                .receiver(userB)
                .status(FriendshipStatus.PENDING)
                .build());

        // when
        // 2. B가 A(requesterId)의 요청을 수락
        friendService.acceptFriend(userB.getId(), userA.getId());

        // then
        // 3. 상태가 FRIENDSHIP으로 변경되었는지 확인
        Friendship friendship = friendshipRepository.findAll().get(0);
        assertThat(friendship.getStatus()).isEqualTo(FriendshipStatus.FRIENDSHIP);
    }

    @Test
    @DisplayName("실패: 존재하지 않는 친구 요청 수락 (FRIEND_REQUEST_NOT_FOUND)")
    void acceptFriend_NotFound() {
        // given
        // (아무 요청도 없는 상태)

        // when & then
        // B가 있지도 않은 A의 요청을 수락하려 함
        assertThatThrownBy(() -> friendService.acceptFriend(userB.getId(), userA.getId()))
                .isInstanceOf(CustomException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.FRIEND_REQUEST_NOT_FOUND);
    }
}