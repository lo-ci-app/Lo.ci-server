package com.teamloci.loci.domain.report;

import com.teamloci.loci.global.auth.AuthenticatedUser;
import com.teamloci.loci.global.common.CustomResponse;
import com.teamloci.loci.global.error.CustomException;
import com.teamloci.loci.global.error.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Report", description = "신고 API")
@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    private Long getUserId(AuthenticatedUser user) {
        if (user == null) throw new CustomException(ErrorCode.UNAUTHORIZED);
        return user.getUserId();
    }

    @Operation(summary = "신고하기", description = "게시물, 댓글, 사용자 등을 신고합니다. (3회 누적 시 자동 블라인드)")
    @PostMapping
    public ResponseEntity<CustomResponse<Void>> createReport(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody ReportDto.CreateRequest request
    ) {
        reportService.createReport(getUserId(user), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CustomResponse.created(null));
    }
}