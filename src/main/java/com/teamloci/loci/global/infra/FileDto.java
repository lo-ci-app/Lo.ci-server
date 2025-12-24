package com.teamloci.loci.global.infra;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class FileDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "일반 파일 업로드 응답")
    public static class FileUploadResponse {
        @Schema(description = "업로드된 파일의 CDN/S3 URL", example = "https://cdn.loci.com/posts/image.jpg")
        private String url;
    }

    @Getter
    @NoArgsConstructor
    @Schema(description = "Presigned URL 발급 요청 DTO")
    public static class PresignedUrlRequest {
        @Schema(description = "업로드할 파일명 (확장자 포함)", example = "my_vlog_video.mp4")
        private String fileName;

        @Schema(description = "저장할 폴더 경로", example = "videos")
        private String directory;
    }

    @Getter
    @AllArgsConstructor
    @Schema(description = "Presigned URL 발급 응답 DTO")
    public static class PresignedUrlResponse {
        @Schema(description = "AWS S3 업로드용 임시 URL (PUT 요청용)", example = "https://loci-assets.s3.ap-northeast-2.amazonaws.com/videos/uuid.mp4?X-Amz-Algorithm=...")
        private String presignedUrl;

        @Schema(description = "업로드 완료 후 DB에 저장해야 할 파일 키(Key)", example = "videos/uuid_my_vlog_video.mp4")
        private String fileKey;
    }
}