package com.teamfiv5.fiv5.controller;

import com.teamfiv5.fiv5.dto.FriendDto;
import com.teamfiv5.fiv5.dto.UserDto;
import com.teamfiv5.fiv5.global.exception.CustomException;
import com.teamfiv5.fiv5.global.exception.code.ErrorCode;
import com.teamfiv5.fiv5.global.response.CustomResponse;
import com.teamfiv5.fiv5.global.security.AuthenticatedUser;
import com.teamfiv5.fiv5.service.FriendService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Friend", description = "친구 관계 및 찾기 API")
@RestController
@RequestMapping("/api/v1/friends")
@RequiredArgsConstructor
public class FriendController {

    private final FriendService friendService;

    // 공통: 사용자 ID 가져오기
    private Long getUserId(AuthenticatedUser user) {
        if (user == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
        return user.getUserId();
    }

    // --- 1. 블루투스 친구 찾기 ---

    @Operation(summary = "[친구] 1. 친구 찾기 시작 (토큰 발급)",
            description = "친구 찾기 기능을 켤 때 호출. 8바이트의 일회용 랜덤 토큰을 발급받습니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "토큰 발급/갱신 성공",
                    content = @Content(examples = @ExampleObject(value = """
                            { "code": "COMMON200", "result": { "bluetoothToken": "gA_j3q-v-m8" } }
                            """))),
            @ApiResponse(responseCode = "401", description = "(COMMON401) 인증 실패", content = @Content)
    })
    @PatchMapping("/discovery/start")
    public ResponseEntity<CustomResponse<FriendDto.DiscoveryTokenResponse>> startFriendDiscovery(
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        Long myUserId = getUserId(user);
        FriendDto.DiscoveryTokenResponse response = friendService.refreshBluetoothToken(myUserId);
        return ResponseEntity.ok(CustomResponse.ok(response));
    }

    @Operation(summary = "[친구] 2. 친구 찾기 중지 (토큰 만료)",
            description = "친구 찾기 기능을 끌 때 호출. 발급받았던 토큰을 즉시 만료시킵니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "토큰 만료 성공",
                    content = @Content(examples = @ExampleObject(value = """
                            { "code": "COMMON200", "result": null }
                            """))),
            @ApiResponse(responseCode = "401", description = "(COMMON401) 인증 실패", content = @Content)
    })
    @PatchMapping("/discovery/stop")
    public ResponseEntity<CustomResponse<Void>> stopFriendDiscovery(
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        Long myUserId = getUserId(user);
        friendService.stopBluetoothToken(myUserId);
        return ResponseEntity.ok(CustomResponse.ok(null));
    }

    @Operation(summary = "[친구] 3. 스캔한 토큰으로 사용자 목록 조회",
            description = "주변에서 스캔한 토큰 리스트를 전송하여, 각 사용자의 프로필과 나와의 친구 상태를 조회합니다.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "스캔한 토큰 문자열 목록", required = true,
            content = @Content(examples = @ExampleObject(value = "{\"tokens\": [\"gA_j3q-v-m8\", \"kLp-2aB_c-1\"]}"))
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공. (친구 상태 포함)",
                    content = @Content(schema = @Schema(implementation = CustomResponse.class),
                            examples = @ExampleObject(value = """
                            {
                              "code": "COMMON200",
                              "result": [
                                {
                                  "id": 2,
                                  "nickname": "즐거운판다",
                                  "bio": "반가워요",
                                  "profileUrl": "https://.../image.png",
                                  "friendshipStatus": "NONE"
                                },
                                {
                                  "id": 3,
                                  "nickname": "행복한쿼카",
                                  "bio": "하이",
                                  "profileUrl": "https://.../image2.png",
                                  "friendshipStatus": "FRIEND"
                                }
                              ]
                            }
                            """)))
    })
    @PostMapping("/discovery/by-tokens")
    public ResponseEntity<CustomResponse<List<FriendDto.DiscoveredUserResponse>>> findUsersByTokens(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody FriendDto.FindFriendsByTokensRequest request
    ) {
        Long myUserId = getUserId(user);
        List<FriendDto.DiscoveredUserResponse> users = friendService.findUsersByTokens(myUserId, request.getTokens());
        return ResponseEntity.ok(CustomResponse.ok(users));
    }

    // --- 2. 친구 요청/수락 ---

    @Operation(summary = "[친구] 4. 친구 요청 (ID 사용)", // (토큰 사용 -> ID 사용)
            description = "특정 사용자의 ID(id)를 사용해 친구 요청을 보냅니다.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "조회 API에서 얻은 상대방의 `id`", required = true,
            content = @Content(examples = @ExampleObject(value = "{\"targetUserId\": 2}"))
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "요청 성공", content = @Content),
            @ApiResponse(responseCode = "400", description = "(FRIEND400_1) 자기 자신에게 요청", content = @Content),
            @ApiResponse(responseCode = "404", description = "(USER404_1) 존재하지 않는 사용자", content = @Content),
            @ApiResponse(responseCode = "409", description = "(FRIEND409_1) 이미 친구 관계 또는 요청 대기 중", content = @Content)
    })
    @PostMapping("/request")
    public ResponseEntity<CustomResponse<Void>> requestFriend(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody FriendDto.FriendManageByIdRequest request
    ) {
        Long myUserId = getUserId(user);
        // (Service 호출 수정) 토큰이 아닌 ID를 전달
        friendService.requestFriend(myUserId, request.getTargetUserId());
        return ResponseEntity.ok(CustomResponse.ok(null));
    }
    @Operation(summary = "[친구] 5. 친구 수락 (ID 사용)",
            description = "나에게 온 친구 요청을 수락합니다. (알림 등에서 받은 요청자의 ID 사용)")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "요청을 보낸 사람의 `id`", required = true,
            content = @Content(examples = @ExampleObject(value = "{\"targetUserId\": 3}"))
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "수락 성공", content = @Content),
            @ApiResponse(responseCode = "404", description = "(FRIEND404_1) 존재하지 않는 친구 요청", content = @Content)
    })
    @PatchMapping("/accept")
    public ResponseEntity<CustomResponse<Void>> acceptFriend(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody FriendDto.FriendManageByIdRequest request // (DTO 수정)
    ) {
        Long myUserId = getUserId(user); // 나는 수신자
        friendService.acceptFriend(myUserId, request.getTargetUserId()); // 상대는 요청자
        return ResponseEntity.ok(CustomResponse.ok(null));
    }

    /**
     * (신규 API 6) 내가 받은 친구 요청 목록 조회
     */
    @Operation(summary = "[친구] 6. 내가 받은 친구 요청 목록 조회",
            description = "나에게 친구 요청을 보냈지만 아직 수락/거절하지 않은 (PENDING) 사용자 목록을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공. (요청이 없으면 빈 리스트 `[]` 반환)",
                    content = @Content(schema = @Schema(implementation = CustomResponse.class),
                            examples = @ExampleObject(value = """
                            {
                              "code": "COMMON200",
                              "result": [
                                {
                                  "id": 1,
                                  "nickname": "행복한쿼카",
                                  "bio": "안녕하세요",
                                  "profileUrl": "https://.../image.png",
                                  "email": "user@apple.com",
                                  "provider": "apple",
                                  "providerId": "001234.abc...",
                                  "createdAt": "2025-11-01T12:00:00"
                                }
                              ]
                            }
                            """))),
            @ApiResponse(responseCode = "401", description = "(COMMON401) 인증 실패", content = @Content)
    })
    @GetMapping("/requests/received") // (신규) GET /api/v1/friends/requests/received
    public ResponseEntity<CustomResponse<List<UserDto.UserResponse>>> getReceivedFriendRequests(
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        Long myUserId = getUserId(user);
        List<UserDto.UserResponse> requesters = friendService.getReceivedFriendRequests(myUserId);
        return ResponseEntity.ok(CustomResponse.ok(requesters));
    }

    /**
     * API 7. 내가 보낸 친구 요청 목록 조회
     */
    @Operation(summary = "[친구] 7. (Pull) 내가 보낸 친구 요청 목록 (수락 대기)",
            description = "내가 친구 요청을 보냈지만 아직 상대방이 수락하지 않은 (PENDING) 사용자 목록을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공", content = @Content),
            @ApiResponse(responseCode = "401", description = "(COMMON401) 인증 실패", content = @Content)
    })
    @GetMapping("/requests/sent")
    public ResponseEntity<CustomResponse<List<UserDto.UserResponse>>> getSentFriendRequests(
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        Long myUserId = getUserId(user);
        List<UserDto.UserResponse> receivers = friendService.getSentFriendRequests(myUserId);
        return ResponseEntity.ok(CustomResponse.ok(receivers));
    }

    /**
     * API 8. 내 친구 목록 조회
     */
    @Operation(summary = "[친구] 8. (Pull) 내 친구 목록",
            description = "서로 친구 관계(FRIENDSHIP)가 수락된 모든 사용자 목록을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공", content = @Content),
            @ApiResponse(responseCode = "401", description = "(COMMON401) 인증 실패", content = @Content)
    })
    @GetMapping("") // GET /api/v1/friends
    public ResponseEntity<CustomResponse<List<UserDto.UserResponse>>> getMyFriends(
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        Long myUserId = getUserId(user);
        List<UserDto.UserResponse> friends = friendService.getMyFriends(myUserId);
        return ResponseEntity.ok(CustomResponse.ok(friends));
    }
}