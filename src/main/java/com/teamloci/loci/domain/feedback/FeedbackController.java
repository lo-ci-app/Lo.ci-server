package com.teamloci.loci.domain.feedback;

import com.teamloci.loci.global.auth.AuthenticatedUser;
import com.teamloci.loci.global.common.CustomResponse; // 기존 CustomResponse 활용
import com.teamloci.loci.global.error.CustomException;
import com.teamloci.loci.global.error.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody; // RequestPart -> RequestBody 변경
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/feedbacks")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;

    private Long getUserId(AuthenticatedUser user) {
        if (user == null) throw new CustomException(ErrorCode.UNAUTHORIZED);
        return user.getUserId();
    }

    @Operation(summary = "피드백 제출", description = "제목, 내용, 이미지 URL을 전송합니다.")
    @PostMapping
    public CustomResponse<String> createFeedback(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestBody FeedbackRequest request) {

        feedbackService.createFeedback(getUserId(user), request);

        return CustomResponse.ok("피드백이 성공적으로 제출되었습니다.");
    }
}