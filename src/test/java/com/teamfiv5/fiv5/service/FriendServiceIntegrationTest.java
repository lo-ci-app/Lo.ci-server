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

import java.util.List;

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

    private User userA;
    private User userB;
    private User userC;

    @BeforeEach
    void setUp() {
        // 테스트 실행 전, H2 DB에 테스트용 유저 3명 생성
        userA = User.builder().nickname("UserA").provider("apple").providerId("provider_A").build();
        userB = User.builder().nickname("UserB").provider("apple").providerId("provider_B").build();
        userC = User.builder().nickname("UserC").provider("apple").providerId("provider_C").build();

        userRepository.saveAll(List.of(userA, userB, userC));
    }

    // --- API 1 (Token) ---
    @Test
    @DisplayName("토큰 발급/갱신 시 유저 DB에 토큰이 저장된다")
    void refreshBluetoothToken_Success() {
        // when
        String tokenA = friendService.refreshBluetoothToken(userA.getId()).getBluetoothToken();

        // then
        User findUserA = userRepository.findById(userA.getId()).get();
        assertThat(findUserA.getBluetoothToken()).isEqualTo(tokenA);
        assertThat(tokenA).isNotNull();
    }

    // --- API 2 (Discovery) ---
    @Test
    @DisplayName("토큰 목록 조회 시 ID, 닉네임, 친구가 아닌 상태(NONE)가 반환된다") // ◀ (수정)
    void findUsersByTokens_Status_NONE() {
        // given
        String tokenB = friendService.refreshBluetoothToken(userB.getId()).getBluetoothToken();
        String tokenC = friendService.refreshBluetoothToken(userC.getId()).getBluetoothToken();

        // when
        // A가 B와 C를 스캔함
        List<FriendDto.DiscoveredUserResponse> result = friendService.findUsersByTokens(
                userA.getId(), List.of(tokenB, tokenC, "fake-token")
        );

        // then
        assertThat(result).hasSize(2);

        // (수정) AssertJ의 extracting()과 containsExactlyInAnyOrder() 사용
        assertThat(result)
                .extracting(FriendDto.DiscoveredUserResponse::getNickname) // 닉네임만 추출
                .containsExactlyInAnyOrder("UserB", "UserC"); // 순서 상관없이 "UserB", "UserC"가 모두 있는지 확인

        // ◀ [추가] DTO가 ID를 잘 반환하는지 검증
        assertThat(result)
                .extracting(FriendDto.DiscoveredUserResponse::getId) // ID 추출
                .containsExactlyInAnyOrder(userB.getId(), userC.getId());

        assertThat(result)
                .extracting(FriendDto.DiscoveredUserResponse::getFriendshipStatus) // 친구 상태만 추출
                .containsOnly(FriendDto.FriendshipStatusInfo.NONE); // 2명 다 "NONE" 상태인지 확인
    }

    // --- API 3 (Request) ---
    @Test
    @DisplayName("성공: A가 B의 ID로 친구 요청을 보낸다") // ◀ (수정)
    void requestFriend_Success() {
        // given
        // ◀ (삭제) String tokenB = friendService.refreshBluetoothToken(userB.getId()).getBluetoothToken();

        // when
        friendService.requestFriend(userA.getId(), userB.getId()); // ◀ (수정) tokenB -> userB.getId()

        // then
        Friendship friendship = friendshipRepository.findAll().get(0);
        assertThat(friendship.getRequester().getId()).isEqualTo(userA.getId());
        assertThat(friendship.getReceiver().getId()).isEqualTo(userB.getId());
        assertThat(friendship.getStatus()).isEqualTo(FriendshipStatus.PENDING);

        // ◀ (삭제) 토큰 만료 로직이 서비스에서 제거되었으므로 테스트도 삭제
        // // (보안) B의 토큰이 만료(null)되었는지 확인 (토큰 재사용 방지)
        // User updatedUserB = userRepository.findById(userB.getId()).get();
        // assertThat(updatedUserB.getBluetoothToken()).isNull();
    }

    @Test
    @DisplayName("실패: A가 자신의 ID로 친구 요청 (SELF_FRIEND_REQUEST)") // ◀ (수정)
    void requestFriend_SelfRequest() {
        // given
        // ◀ (삭제) String tokenA = friendService.refreshBluetoothToken(userA.getId()).getBluetoothToken();

        // when & then
        assertThatThrownBy(() -> friendService.requestFriend(userA.getId(), userA.getId())) // ◀ (수정) tokenA -> userA.getId()
                .isInstanceOf(CustomException.class)
                .extracting("code") // CustomException에서 ErrorCode 객체를 가져옴
                .isEqualTo(ErrorCode.SELF_FRIEND_REQUEST); //
    }

    @Test
    @DisplayName("실패: A가 이미 친구인 B에게 ID로 다시 요청 (FRIEND_REQUEST_ALREADY_EXISTS)") // ◀ (수정)
    void requestFriend_AlreadyExists() {
        // given
        // 1. A와 B가 이미 친구 상태
        friendshipRepository.save(Friendship.builder()
                .requester(userA)
                .receiver(userB)
                .status(FriendshipStatus.FRIENDSHIP)
                .build());

        // when & then
        // 3. A가 B의 ID로 다시 요청 시 예외 발생
        assertThatThrownBy(() -> friendService.requestFriend(userA.getId(), userB.getId())) // ◀ (수정) tokenB_new -> userB.getId()
                .isInstanceOf(CustomException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.FRIEND_REQUEST_ALREADY_EXISTS); //
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
                .isEqualTo(ErrorCode.FRIEND_REQUEST_NOT_FOUND); //
    }
}