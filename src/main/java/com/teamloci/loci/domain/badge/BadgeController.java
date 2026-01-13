package com.teamloci.loci.domain.badge;

import com.teamloci.loci.global.auth.AuthenticatedUser;
import com.teamloci.loci.global.error.CustomException;
import com.teamloci.loci.global.error.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Badges", description = "배지 시스템 API")
@RestController
@RequestMapping("/api/v1/badges")
@RequiredArgsConstructor
public class BadgeController {

    private final BadgeService badgeService;

    private Long getUserId(AuthenticatedUser user) {
        if (user == null) throw new CustomException(ErrorCode.UNAUTHORIZED);
        return user.getUserId();
    }

    @Operation(summary = "내 배지 목록 조회", description = "나의 모든 배지 획득 현황을 조회합니다.\n" +
            "- 국가 코드(KR/EN)에 따라 이름과 설명이 자동으로 변환됩니다.\n" +
            "- 미획득 배지는 '???' 및 잠금 이미지로 표시됩니다.\n" +
            "- `isMain`이 true인 배지가 현재 장착 중인 배지입니다.")
    @GetMapping
    public ResponseEntity<List<BadgeResponse>> getMyBadges(@AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(badgeService.getBadgeList(getUserId(user)));
    }

    @Operation(summary = "메인 배지 선택 (장착)", description = "프로필과 피드에 노출될 대표 배지를 변경합니다.\n" +
            "- 획득하지 못한 배지를 선택하면 400 에러가 발생합니다.")
    @PatchMapping("/{badgeId}/main")
    public ResponseEntity<Void> setMainBadge(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long badgeId) {
        badgeService.setMainBadge(getUserId(user), badgeId);
        return ResponseEntity.ok().build();
    }
}