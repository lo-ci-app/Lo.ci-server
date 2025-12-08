package com.teamloci.loci.domain.friend;

import com.teamloci.loci.domain.user.UserDto;
import com.teamloci.loci.global.error.CustomException;
import com.teamloci.loci.global.error.ErrorCode;
import com.teamloci.loci.global.common.CustomResponse;
import com.teamloci.loci.global.auth.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Friend", description = "친구 관리 API (요청/수락/거절 및 검색)")
@RestController
@RequestMapping("/api/v1/friends")
@RequiredArgsConstructor
public class FriendController {

    private final FriendService friendService;

    private Long getUserId(AuthenticatedUser user) {
        if (user == null) throw new CustomException(ErrorCode.UNAUTHORIZED);
        return user.getUserId();
    }

    @Operation(summary = "친구 요청 보내기",
            description = """
                    특정 유저에게 친구 요청을 보냅니다.
                    
                    **[로직]**
                    * 상태가 `PENDING`으로 생성됩니다.
                    * 상대방에게 알림이 전송됩니다.
                    * 만약 상대방이 이미 나에게 요청을 보낸 상태라면, 자동으로 **친구 수락(FRIENDSHIP)** 처리됩니다.
                    """)
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(examples = @ExampleObject(value = "{\"targetUserId\": 3}"))
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "요청 성공"),
            @ApiResponse(responseCode = "409", description = "(FRIEND409_1) 이미 친구이거나 요청 대기 중"),
            @ApiResponse(responseCode = "409", description = "(FRIEND409_2) 내 친구 수 초과 (최대 20명)")
    })
    @PostMapping("/request")
    public ResponseEntity<CustomResponse<Void>> sendFriendRequest(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody FriendDto.FriendManageByIdRequest request
    ) {
        friendService.sendFriendRequest(getUserId(user), request.getTargetUserId());
        return ResponseEntity.ok(CustomResponse.ok(null));
    }

    @Operation(summary = "친구 요청 수락",
            description = """
                    나에게 온 친구 요청을 수락합니다.
                    
                    * `targetUserId`: 나에게 요청을 보낸 사람의 ID
                    * 상태가 `PENDING` -> `FRIENDSHIP`으로 변경됩니다.
                    """)
    @PostMapping("/accept")
    public ResponseEntity<CustomResponse<Void>> acceptFriendRequest(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody FriendDto.FriendManageByIdRequest request
    ) {
        friendService.acceptFriendRequest(getUserId(user), request.getTargetUserId());
        return ResponseEntity.ok(CustomResponse.ok(null));
    }

    @Operation(summary = "친구 삭제 / 요청 취소 / 거절",
            description = """
                    친구 관계, 보낸 요청, 받은 요청을 모두 삭제합니다.
                    
                    * **이미 친구인 경우:** 절교 (Unfriend)
                    * **내가 요청을 보낸 경우:** 요청 취소
                    * **요청을 받은 경우:** 요청 거절
                    """)
    @DeleteMapping
    public ResponseEntity<CustomResponse<Void>> deleteFriendship(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody FriendDto.FriendManageByIdRequest request
    ) {
        friendService.deleteFriendship(getUserId(user), request.getTargetUserId());
        return ResponseEntity.ok(CustomResponse.ok(null));
    }

    @Operation(summary = "받은 친구 요청 목록", description = "나에게 친구 요청을 보낸(수락 대기 중인) 유저 목록을 조회합니다.")
    @GetMapping("/requests/received")
    public ResponseEntity<CustomResponse<List<UserDto.UserResponse>>> getReceivedRequests(
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ResponseEntity.ok(CustomResponse.ok(friendService.getReceivedRequests(getUserId(user))));
    }

    @Operation(summary = "보낸 친구 요청 목록", description = "내가 친구 요청을 보냈으나 아직 수락되지 않은 목록을 조회합니다.")
    @GetMapping("/requests/sent")
    public ResponseEntity<CustomResponse<List<UserDto.UserResponse>>> getSentRequests(
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ResponseEntity.ok(CustomResponse.ok(friendService.getSentRequests(getUserId(user))));
    }

    @Operation(summary = "내 친구 목록 (친밀도 포함)",
            description = "서로 친구(FRIENDSHIP) 상태인 유저 목록을 반환합니다. `intimacyLevel`, `intimacyScore` 필드가 포함됩니다.")
    @GetMapping
    public ResponseEntity<CustomResponse<List<UserDto.UserResponse>>> getMyFriends(
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ResponseEntity.ok(CustomResponse.ok(friendService.getMyFriends(getUserId(user))));
    }

    @Operation(summary = "유저 검색 (Cursor Pagination)",
            description = """
                    핸들(@ID) 또는 닉네임으로 유저를 검색합니다.
                    **커서 기반 페이지네이션**이 적용되었습니다.
                    
                    * `cursorId`: 마지막으로 조회된 유저의 ID (첫 요청 시 비워두세요)
                    """)
    @GetMapping("/search")
    public ResponseEntity<CustomResponse<UserDto.UserSearchResponse>> searchUsers(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Parameter(description = "검색어", required = true) @RequestParam String keyword,
            @Parameter(description = "이전 페이지의 마지막 유저 ID (첫 요청 시 null)") @RequestParam(required = false) Long cursorId,
            @Parameter(description = "가져올 개수") @RequestParam(defaultValue = "10") int size
    ) {
        Long myUserId = getUserId(user);
        return ResponseEntity.ok(CustomResponse.ok(friendService.searchUsers(myUserId, keyword, cursorId, size)));
    }

    @Operation(summary = "연락처 기반 매칭 (동기화)",
            description = """
                    전화번호 리스트(이름 포함)를 서버에 **저장(동기화)** 하고, 가입된 친구를 찾습니다.
                    **[동작 방식]**
                    1. 기존 연락처와 비교하여 변경된 부분(추가/삭제/수정)을 DB에 반영합니다. (Upsert)
                    2. 저장된 연락처 중 앱에 가입된 유저 목록을 반환합니다.
                    """)
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(examples = @ExampleObject(value = """
                    {
                      "contacts": [
                        { "name": "김철수", "phoneNumber": "010-1234-5678" },
                        { "name": "이영희", "phoneNumber": "+82 10-9999-8888" }
                      ]
                    }
                    """))
    )
    @PostMapping("/match")
    public ResponseEntity<CustomResponse<List<UserDto.UserResponse>>> matchFriends(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody FriendDto.ContactListRequest request
    ) {
        return ResponseEntity.ok(CustomResponse.ok(friendService.matchFriends(getUserId(user), request.getContacts())));
    }

    @Operation(summary = "저장된 연락처 목록 불러오기", description = "서버에 동기화된 내 연락처 목록을 조회합니다.")
    @GetMapping("/contacts")
    public ResponseEntity<CustomResponse<List<FriendDto.ContactResponse>>> getSyncedContacts(
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ResponseEntity.ok(CustomResponse.ok(friendService.getSyncedContacts(getUserId(user))));
    }
}