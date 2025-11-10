// 경로: src/main/java/com/teamfiv5/fiv5/service/S3UploadService.java
package com.teamfiv5.fiv5.service;

import com.teamfiv5.fiv5.global.exception.CustomException;
import com.teamfiv5.fiv5.global.exception.code.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3UploadService {

    // (자동 주입) 'spring-cloud-aws-starter-s3'가 S3Client Bean을 자동으로 생성해 줍니다.
    private final S3Client s3Client;

    @Value("${spring.cloud.aws.s3.bucket:fiv5-assets}")
    private String bucket;

    /**
     * * @param newFile    새로 업로드할 파일 (null이나 empty면 "삭제"로 간주)
     * @param oldFileUrl DB에 저장되어 있던 기존 파일 URL (삭제 대상)
     * @param dirName    S3 디렉토리 (예: "profiles", "posts")
     * @return S3에 저장된 새 파일의 URL (삭제된 경우 null)
     */
    @Transactional
    public String uploadAndReplace(MultipartFile newFile, String oldFileUrl, String dirName) {
        String newFileUrl = null;

        // 1. (변경/추가) 새 파일이 들어온 경우
        if (newFile != null && !newFile.isEmpty()) {
            newFileUrl = this.upload(newFile, dirName); // (내부 upload 메서드 호출)
        }
        // 2. (삭제) 새 파일이 안 들어온 경우 -> null (삭제)

        // 3. (중요) 기존 파일(oldFileUrl)이 있었고, 새 파일과 URL이 다르다면
        //    (새 URL이 null로(삭제) 변경되었거나, 새 URL이 기존과 다를 때)
        if (oldFileUrl != null && (newFileUrl == null || !oldFileUrl.equals(newFileUrl))) {
            this.delete(oldFileUrl); // (내부 delete 메서드 호출)
        }

        return newFileUrl; // DB에 저장할 최종 URL 반환
    }

    /**
     * * @param newFileUrl 프론트가 제공한 새 S3 URL
     * @param oldFileUrl DB에 저장되어 있던 기존 파일 URL (삭제 대상)
     */
    @Transactional
    public void replaceUrl(String newFileUrl, String oldFileUrl) {
        // (중요) 기존 파일(oldFileUrl)이 있었고, 새 파일(newFileUrl)과 다르다면
        if (oldFileUrl != null && (newFileUrl == null || !oldFileUrl.equals(newFileUrl))) {
            // (참고) 이 로직은 프론트가 S3에서 직접 삭제했다는 가정 하에,
            // 백엔드는 S3에서 delete를 호출하지 *않습니다*.
            // 만약 백엔드도 삭제해야 한다면 this.delete(oldFileUrl); 호출
            System.out.println("기존 S3 파일 삭제 필요(로직상 스킵): " + oldFileUrl);
        }
    }

    /**
     * 파일을 S3에 업로드하고, 생성된 URL을 반환합니다.
     * @param file 업로드할 파일
     * @param dirName 버킷 내의 디렉토리 이름 (예: "profiles")
     * @return S3에 업로드된 파일의 전체 URL
     */
    public String upload(MultipartFile file, String dirName) {
        if (file == null || file.isEmpty()) {
            throw new CustomException(ErrorCode.FILE_IS_EMPTY);
        }

        String original = Optional.ofNullable(file.getOriginalFilename())
                .filter(name -> !name.isBlank())
                .orElseThrow(() -> new IllegalArgumentException("파일 이름이 없습니다."));

        // 파일 이름 중복 방지를 위해 UUID 사용
        String uniqueName = UUID.randomUUID() + "_" + original.replaceAll("[^a-zA-Z0-9.\\-]", "_");
        String key = dirName + "/" + uniqueName; // S3 내 최종 경로 (예: profiles/uuid_image.jpg)

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(file.getContentType())
                .cacheControl("public, max-age=31536000") // 1년간 캐시
                .build();

        try {
            s3Client.putObject(putObjectRequest,
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        } catch (SdkException | IOException e) {
            throw new CustomException(ErrorCode.S3_UPLOAD_FAILED);
        }

        // 업로드된 파일의 URL 반환
        return s3Client.utilities().getUrl(builder -> builder.bucket(bucket).key(key)).toString();
    }

    /**
     * S3에서 파일을 삭제합니다.
     * @param fileUrl 삭제할 파일의 전체 URL
     */
    public void delete(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) {
            return;
        }

        try {
            // URL에서 S3 'key' (파일 경로) 추출
            String key = fileUrl.substring(fileUrl.indexOf(bucket) + bucket.length() + 1);

            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);

        } catch (Exception e) {
            // 삭제 실패 시 로그만 남김 (이미 삭제되었거나, URL이 잘못되었을 수 있음)
            System.err.println("S3 파일 삭제 실패: " + fileUrl + " (" + e.getMessage() + ")");
        }
    }
}