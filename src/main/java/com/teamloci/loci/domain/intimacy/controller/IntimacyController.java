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

    @Operation(summary = "친밀도 상세 조회",
            description = """
                    특정 유저와의 **친밀도 상태**와 **나의 전체 레벨 합** 정보를 조회합니다.
                    
                    ### [Response Fields 설명]
                    * **level**: 현재 이 친구와의 친밀도 레벨 (예: 3)
                    * **totalScore**: 현재까지 쌓은 누적 점수 (예: 250) -> **게이지 바 현재 값**
                    * **nextLevelScore**: 다음 레벨이 되기 위한 목표 점수 (예: 376) -> **게이지 바 최대 값**
                    * **myTotalLevel**: 내 모든 친구 관계의 레벨 총합 (예: 15) -> **마이페이지/메인 스탯용**
                    * **targetUser**: 상대방의 프로필 정보
                    """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "code": "COMMON200",
                              "message": "성공적으로 요청을 수행했습니다.",
                              "result": {
                                "targetUser": {
                                  "id": 2,
                                  "handle": "friend_handle",
                                  "nickname": "단짝친구",
                                  "profileUrl": "https://loci-assets.s3.ap-northeast-2.amazonaws.com/profiles/sample.jpg",
                                  "relationStatus": "FRIEND",
                                  "friendCount": 150,
                                  "postCount": 20
                                },
                                "level": 3,
                                "totalScore": 250,
                                "nextLevelScore": 376,
                                "myTotalLevel": 15
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