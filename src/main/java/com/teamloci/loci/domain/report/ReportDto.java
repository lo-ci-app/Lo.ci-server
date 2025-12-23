package com.teamloci.loci.domain.report;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class ReportDto {

    @Getter
    @NoArgsConstructor
    @Schema(description = "신고 생성 요청")
    public static class CreateRequest {

        @Schema(description = "신고 대상 유형 (POST, COMMENT)", example = "POST")
        @NotNull(message = "신고 대상 유형은 필수입니다.")
        private ReportTarget targetType;

        @Schema(description = "신고 대상 ID (게시물 ID, 댓글 ID 등)", example = "123")
        @NotNull(message = "신고 대상 ID는 필수입니다.")
        private Long targetId;

        @Schema(description = "신고 사유 (SPAM, ABUSIVE_LANGUAGE...)", example = "INAPPROPRIATE_CONTENT")
        @NotNull(message = "신고 사유는 필수입니다.")
        private ReportReason reason;

        @Schema(description = "상세 설명 (선택 사항)", example = "부적절한 홍보물이 포함되어 있습니다.")
        private String description;
    }
}