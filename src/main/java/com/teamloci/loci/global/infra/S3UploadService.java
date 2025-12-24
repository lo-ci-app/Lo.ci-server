package com.teamloci.loci.global.infra;

import com.teamloci.loci.global.error.CustomException;
import com.teamloci.loci.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.util.Optional;
import java.util.Set; // [Change] List -> Set
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3UploadService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    private static final String CACHE_CONTROL_VALUE = "public, max-age=2592000";
    private static final String CLOUDFRONT_DOMAIN = "https://dagvorl6p9q6m.cloudfront.net";
    private static final Set<String> ALLOWED_VIDEO_EXTENSIONS = Set.of("mp4", "mov", "avi", "wmv", "mkv", "webm");
    private static final long MAX_FILE_SIZE = 500 * 1024 * 1024;

    @Value("${spring.cloud.aws.s3.bucket:loci-assets}")
    private String bucket;

    @Value("${spring.cloud.aws.s3.presigned-url.duration:10}")
    private long presignedUrlDuration;

    @Transactional
    public String uploadAndReplace(MultipartFile newFile, String oldFileUrl, String dirName) {
        String newFileUrl = null;

        if (newFile != null && !newFile.isEmpty()) {
            newFileUrl = this.upload(newFile, dirName);
        }

        if (oldFileUrl != null && (newFileUrl == null || !oldFileUrl.equals(newFileUrl))) {
            this.delete(oldFileUrl);
        }

        return newFileUrl;
    }

    @Transactional
    public void replaceUrl(String newFileUrl, String oldFileUrl) {
        if (oldFileUrl != null && (newFileUrl == null || !oldFileUrl.equals(newFileUrl))) {
            this.delete(oldFileUrl);
        }
    }

    public String upload(MultipartFile file, String dirName) {
        if (file == null || file.isEmpty()) {
            throw new CustomException(ErrorCode.FILE_IS_EMPTY);
        }

        String original = Optional.ofNullable(file.getOriginalFilename())
                .filter(name -> !name.isBlank())
                .orElseThrow(() -> new CustomException(ErrorCode.FILE_NAME_INVALID));

        String sanitizedOriginal = original.replaceAll("[^\\p{L}\\p{N}.\\-]", "_");
        String uniqueName = UUID.randomUUID() + "_" + sanitizedOriginal;
        String key = dirName + "/" + uniqueName;

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(file.getContentType())
                .cacheControl(CACHE_CONTROL_VALUE)
                .build();

        try {
            s3Client.putObject(putObjectRequest,
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        } catch (SdkException | IOException e) {
            throw new CustomException(ErrorCode.S3_UPLOAD_FAILED);
        }

        return CLOUDFRONT_DOMAIN + "/" + key;
    }

    public void delete(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) {
            return;
        }

        try {
            URL url = new URL(fileUrl);
            String key = url.getPath();

            if (key.startsWith("/")) {
                key = key.substring(1);
            }

            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);

        } catch (Exception e) {
            log.error("S3 파일 삭제 실패: {} ({})", fileUrl, e.getMessage());
        }
    }

    public FileDto.PresignedUrlResponse getPresignedUrl(String directory, String fileName, Long fileSize) {
        String ext = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();

        if (!ALLOWED_VIDEO_EXTENSIONS.contains(ext)) {
            throw new CustomException(ErrorCode.FILE_NAME_INVALID);
        }

        if (fileSize == null || fileSize > MAX_FILE_SIZE) {
            throw new CustomException(ErrorCode.FILE_SIZE_EXCEEDED);
        }

        String sanitizedFileName = fileName.replaceAll("[^\\p{L}\\p{N}.\\-]", "_");

        String uniqueFileName = directory + "/" + UUID.randomUUID() + "_" + sanitizedFileName;

        String contentType = switch (ext) {
            case "mov" -> "video/quicktime";
            case "avi" -> "video/x-msvideo";
            case "wmv" -> "video/x-ms-wmv";
            case "mkv" -> "video/x-matroska";
            case "webm" -> "video/webm";
            default -> "video/" + ext; // mp4 등
        };

        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(uniqueFileName)
                .contentType(contentType)
                .contentLength(fileSize)
                .cacheControl("public, max-age=31536000")
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(presignedUrlDuration))
                .putObjectRequest(objectRequest)
                .build();

        String url = s3Presigner.presignPutObject(presignRequest).url().toString();

        return new FileDto.PresignedUrlResponse(url, uniqueFileName);
    }
}