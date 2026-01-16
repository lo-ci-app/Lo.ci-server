package com.teamloci.loci.domain.badge;

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
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Badge", description = "배지 API")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class BadgeController {

    private final BadgeService badgeService;

    private Long getUserId(AuthenticatedUser user) {
        if (user == null) throw new CustomException(ErrorCode.UNAUTHORIZED);
        return user.getUserId();
    }

    @Operation(summary = "내 배지 목록 조회", description = "로그인한 유저(본인)의 획득 및 미획득 배지 전체 목록을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "timestamp": "2026-01-13T12:38:56.055Z",
                                      "isSuccess": true,
                                      "code": "COMMON200",
                                      "message": "성공적으로 요청을 수행했습니다.",
                                      "result": [
                                        {
                                          "userId": 1,
                                          "id": 1,
                                          "name": "뉴비",
                                          "condition": "첫 로그인",
                                          "description": "세상아, 안녕!",
                                          "imageUrl": "https://dagvorl6p9q6m.cloudfront.net/badge/newbie_badge.png",
                                          "isAcquired": true,
                                          "isMain": true
                                        },
                                        {
                                          "userId": 1,
                                          "id": 2,
                                          "name": "???",
                                          "condition": "Lo.ci 베타테스트에 참여",
                                          "description": "조건을 달성하여 배지를 획득하세요.",
                                          "imageUrl": "https://dagvorl6p9q6m.cloudfront.net/badge/locked_badge.png",
                                          "isAcquired": false,
                                          "isMain": false
                                        }
                                      ]
                                    }
                                    """)))
    })
    @GetMapping("/me/badges")
    public ResponseEntity<CustomResponse<List<BadgeResponse>>> getMyBadges(
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ResponseEntity.ok(CustomResponse.ok(
                badgeService.getBadgeList(getUserId(user))
        ));
    }

    @Operation(summary = "특정 유저 배지 목록 조회", description = "다른 유저(또는 특정 ID)의 배지 목록을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "timestamp": "2026-01-13T12:40:12.123Z",
                                      "isSuccess": true,
                                      "code": "COMMON200",
                                      "message": "성공적으로 요청을 수행했습니다.",
                                      "result": [
                                        {
                                          "userId": 15,
                                          "id": 1,
                                          "name": "뉴비",
                                          "condition": "첫 로그인",
                                          "description": "세상아, 안녕!",
                                          "imageUrl": "https://dagvorl6p9q6m.cloudfront.net/badge/newbie_badge.png",
                                          "isAcquired": true,
                                          "isMain": false
                                        },
                                        {
                                          "userId": 15,
                                          "id": 8,
                                          "name": "터줏대감",
                                          "condition": "특정 비콘에서 게시글 30개",
                                          "description": "이불 밖은 위험해.",
                                          "imageUrl": "https://dagvorl6p9q6m.cloudfront.net/badge/the_landlord_badge.png",
                                          "isAcquired": true,
                                          "isMain": true
                                        }
                                      ]
                                    }
                                    """)))
    })
    @GetMapping("/{userId}/badges")
    public ResponseEntity<CustomResponse<List<BadgeResponse>>> getUserBadges(
            @Parameter(description = "조회할 유저의 ID", example = "1", required = true)
            @PathVariable Long userId
    ) {
        return ResponseEntity.ok(CustomResponse.ok(
                badgeService.getBadgeList(userId)
        ));
    }

    @Operation(summary = "메인 배지 변경", description = "내 프로필에 표시될 메인 배지를 변경합니다. 획득한 배지만 설정 가능합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "변경 성공",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "timestamp": "2026-01-13T12:42:55.777Z",
                              "isSuccess": true,
                              "code": "COMMON200",
                              "message": "성공적으로 요청을 수행했습니다.",
                              "result": null
                            }
                            """)))
    })
    @PatchMapping("/me/main-badge/{badgeId}")
    public ResponseEntity<CustomResponse<Void>> updateMainBadge(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Parameter(description = "장착할 배지 ID", example = "3", required = true)
            @PathVariable Long badgeId
    ) {
        badgeService.setMainBadge(getUserId(user), badgeId);
        return ResponseEntity.ok(CustomResponse.ok(null));
    }
}