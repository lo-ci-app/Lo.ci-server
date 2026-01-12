package com.teamloci.loci.domain.feedback;

import com.teamloci.loci.global.auth.AuthenticatedUser;
import com.teamloci.loci.global.error.CustomException;
import com.teamloci.loci.global.error.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/feedbacks")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;

    private Long getUserId(AuthenticatedUser user) {
        if (user == null) throw new CustomException(ErrorCode.UNAUTHORIZED);
        return user.getUserId();
    }

    @Operation(summary = "피드백 제출", description = "제목, 내용, 이미지를 전송합니다. (Multipart/form-data)")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> createFeedback(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestPart("request") FeedbackRequest request,
            @Parameter(description = "이미지 파일 (선택)", content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE))
            @RequestPart(value = "image", required = false) MultipartFile image) {

        feedbackService.createFeedback(getUserId(user), request, image);
        return ResponseEntity.ok("피드백이 성공적으로 제출되었습니다.");
    }
}