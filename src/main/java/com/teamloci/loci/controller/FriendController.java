package com.teamloci.loci.controller;

import com.teamloci.loci.dto.FriendDto;
import com.teamloci.loci.dto.UserDto;
import com.teamloci.loci.global.exception.CustomException;
import com.teamloci.loci.global.exception.code.ErrorCode;
import com.teamloci.loci.global.response.CustomResponse;
import com.teamloci.loci.global.security.AuthenticatedUser;
import com.teamloci.loci.service.FriendService;
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

@Tag(name = "Friend", description = "친구 관리 API (연락처 매칭, 친구 추가/삭제, 목록 조회)")
@RestController
@RequestMapping("/api/v1/friends")
@RequiredArgsConstructor
public class FriendController {

    private final FriendService friendService;

    private Long getUserId(AuthenticatedUser user) {
        if (user == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
        return user.getUserId();
    }

    @Operation(summary = "[친구] 1. 연락처 기반 친구 매칭 (Contact Sync)",
            description = """
                사용자의 휴대폰 주소록에 있는 전화번호 리스트를 전송하면, **우리 앱에 가입된 친구 목록**을 반환합니다.
                
                **[기능 상세]**
                * 클라이언트는 주소록의 전화번호를 **있는 그대로(Raw)** 보내도 됩니다. (예: `010-1234-5678`, `+82 10 1234 5678`)
                * 서버에서 사용자의 국가 코드(Country Code)를 기준으로 **E.164 국제 표준 포맷**으로 변환하고 해싱하여 매칭합니다.
                * **나 자신**과 **탈퇴한 사용자**는 결과에서 자동으로 제외됩니다.
                * 이미 친구인 사용자도 포함되어 반환될 수 있습니다.
                """)
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "매칭할 전화번호 리스트",
            required = true,
            content = @Content(examples = @ExampleObject(value = """
                    {
                      "phoneNumbers": [
                        "010-1234-5678",
                        "+82 10-9876-5432",
                        "010 5555 7777"
                      ]
                    }
                    """))
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "매칭 성공 (가입된 유저 리스트 반환)",
                    content = @Content(schema = @Schema(implementation = CustomResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "code": "COMMON200",
                                      "result": [
                                        {
                                          "id": 3,
                                          "nickname": "내친구1",
                                          "bio": "반가워",
                                          "profileUrl": "https://...",
                                          "email": "friend1@test.com",
                                          "provider": "phone",
                                          "providerId": "firebase_uid_1",
                                          "createdAt": "2025-11-20T10:00:00"
                                        }
                                      ]
                                    }
                                    """))),
            @ApiResponse(responseCode = "400", description = "(COMMON400) 전화번호 리스트가 없거나 비어있음", content = @Content),
            @ApiResponse(responseCode = "401", description = "(COMMON401) 인증 실패", content = @Content)
    })
    @PostMapping("/match")
    public ResponseEntity<CustomResponse<List<UserDto.UserResponse>>> matchFriends(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody FriendDto.ContactListRequest request
    ) {
        Long myUserId = getUserId(user);
        List<UserDto.UserResponse> matchedFriends = friendService.matchFriends(myUserId, request.getPhoneNumbers());
        return ResponseEntity.ok(CustomResponse.ok(matchedFriends));
    }

    @Operation(summary = "[친구] 2. 친구 추가 (즉시 연결)",
            description = """
                매칭된 유저나 특정 유저의 ID(`targetUserId`)로 친구를 맺습니다.
                별도의 수락 대기(Pending) 과정 없이 **즉시 친구 관계(Friendship)**가 됩니다.
                """)
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "친구 추가할 대상의 ID",
            required = true,
            content = @Content(examples = @ExampleObject(value = """
                    {
                      "targetUserId": 3
                    }
                    """))
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "친구 추가 성공",
                    content = @Content(schema = @Schema(implementation = CustomResponse.class))),
            @ApiResponse(responseCode = "400", description = "(FRIEND400_1) 자기 자신을 추가할 수 없음", content = @Content),
            @ApiResponse(responseCode = "404", description = "(USER404_1) 존재하지 않는 사용자 ID", content = @Content),
            @ApiResponse(responseCode = "409", description = "(FRIEND409_1) 이미 친구 관계임 / (FRIEND409_2) 내 친구 수 초과", content = @Content)
    })
    @PostMapping
    public ResponseEntity<CustomResponse<Void>> addFriend(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody FriendDto.FriendManageByIdRequest request
    ) {
        Long myUserId = getUserId(user);
        friendService.addFriend(myUserId, request.getTargetUserId());
        return ResponseEntity.ok(CustomResponse.ok(null));
    }

    @Operation(summary = "[친구] 3. 내 친구 목록 조회",
            description = "현재 나와 친구 관계(FRIENDSHIP)인 모든 사용자 목록을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = CustomResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "code": "COMMON200",
                                      "result": [
                                        {
                                          "id": 3,
                                          "nickname": "내친구1",
                                          ...
                                        },
                                        {
                                          "id": 5,
                                          "nickname": "베프",
                                          ...
                                        }
                                      ]
                                    }
                                    """)))
    })
    @GetMapping
    public ResponseEntity<CustomResponse<List<UserDto.UserResponse>>> getMyFriends(
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        Long myUserId = getUserId(user);
        List<UserDto.UserResponse> friends = friendService.getMyFriends(myUserId);
        return ResponseEntity.ok(CustomResponse.ok(friends));
    }

    @Operation(summary = "[친구] 4. 친구 삭제 (언프렌드)",
            description = "특정 유저와의 친구 관계를 끊습니다.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "삭제할 친구의 ID",
            required = true,
            content = @Content(examples = @ExampleObject(value = """
                    {
                      "targetUserId": 3
                    }
                    """))
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "삭제 성공",
                    content = @Content(schema = @Schema(implementation = CustomResponse.class))),
            @ApiResponse(responseCode = "404", description = "(FRIEND404_3) 친구 관계가 존재하지 않음", content = @Content)
    })
    @DeleteMapping
    public ResponseEntity<CustomResponse<Void>> deleteFriend(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody FriendDto.FriendManageByIdRequest request
    ) {
        Long myUserId = getUserId(user);
        friendService.deleteFriend(myUserId, request.getTargetUserId());
        return ResponseEntity.ok(CustomResponse.ok(null));
    }
}