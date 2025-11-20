package com.teamloci.loci.service;

import com.teamloci.loci.global.exception.CustomException;
import com.teamloci.loci.global.exception.code.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3UploadService {

    private final S3Client s3Client;
    private static final String CACHE_CONTROL_VALUE = "public, max-age=2592000";

    @Value("${spring.cloud.aws.s3.bucket:loci-assets}")
    private String bucket;

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
                .orElseThrow(() -> new IllegalArgumentException("파일 이름이 없습니다."));

        String uniqueName = UUID.randomUUID() + "_" + original.replaceAll("[^a-zA-Z0-9.\\-]", "_");
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

        return s3Client.utilities().getUrl(builder -> builder.bucket(bucket).key(key)).toString();
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
            System.err.println("S3 파일 삭제 실패: " + fileUrl + " (" + e.getMessage() + ")");
        }
    }

    public boolean doesObjectExist(String key) {
        try {
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();
            s3Client.headObject(headObjectRequest);
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        }
    }

    public byte[] downloadFile(String key) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();
        return s3Client.getObject(getObjectRequest, ResponseTransformer.toBytes()).asByteArray();
    }

    public void uploadBytes(byte[] bytes, String key, String contentType) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .cacheControl("public, max-age=31536000")
                .build();

        s3Client.putObject(putObjectRequest,
                RequestBody.fromInputStream(new ByteArrayInputStream(bytes), bytes.length));
    }

    public String extractKeyFromUrl(String fileUrl) {
        try {
            URL url = new URL(fileUrl);
            String key = url.getPath();
            if (key.startsWith("/")) {
                key = key.substring(1);
            }
            return key;
        } catch (Exception e) {
            return fileUrl;
        }
    }
}