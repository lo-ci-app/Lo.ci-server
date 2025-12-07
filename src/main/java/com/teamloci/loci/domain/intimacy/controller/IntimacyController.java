package com.teamloci.loci.domain.intimacy.controller;

import com.teamloci.loci.domain.intimacy.dto.IntimacyDto;
import com.teamloci.loci.domain.intimacy.service.IntimacyService;
import com.teamloci.loci.global.auth.AuthenticatedUser;
import com.teamloci.loci.global.common.CustomResponse;
import com.teamloci.loci.global.error.CustomException;
import com.teamloci.loci.global.error.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Intimacy", description = "친밀도 관리 API")
@RestController
@RequestMapping("/api/v1/intimacy")
@RequiredArgsConstructor
public class IntimacyController {

    private final IntimacyService intimacyService;

    private Long getUserId(AuthenticatedUser user) {
        if (user == null) throw new CustomException(ErrorCode.UNAUTHORIZED);
        return user.getUserId();
    }

    @Operation(summary = "친밀도 조회 (With 유저 정보)",
            description = """
                    특정 유저와의 친밀도 레벨, 점수, 그리고 상대방의 프로필 정보를 함께 조회합니다.
                    친밀도 기록이 없는 경우 점수 0, 레벨 1로 반환됩니다.
                    """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "code": "COMMON200",
                              "result": {
                                "targetUser": {
                                  "id": 2,
                                  "handle": "friend_handle",
                                  "nickname": "단짝친구",
                                  "profileUrl": "https://...",
                                  "relationStatus": "FRIEND",
                                  "friendCount": 150,
                                  "postCount": 20
                                },
                                "level": 3,
                                "score": 250
                              }
                            }
                            """))),
            @ApiResponse(responseCode = "404", description = "유저를 찾을 수 없음")
    })
    @GetMapping("/{targetUserId}")
    public ResponseEntity<CustomResponse<IntimacyDto.DetailResponse>> getIntimacy(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Parameter(description = "상대방 유저 ID", required = true) @PathVariable Long targetUserId
    ) {
        return ResponseEntity.ok(CustomResponse.ok(
                intimacyService.getIntimacyDetail(getUserId(user), targetUserId)
        ));
    }
}