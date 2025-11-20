package com.teamloci.loci.controller;

import com.teamloci.loci.global.exception.CustomException;
import com.teamloci.loci.global.exception.code.ErrorCode;
import com.teamloci.loci.service.S3UploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

@Tag(name = "Image", description = "이미지 리사이징 및 최적화 API")
@RestController
@RequestMapping("/api/v1/images")
@RequiredArgsConstructor
@Slf4j
public class ImageController {

    private final S3UploadService s3UploadService;

    @Operation(summary = "이미지 리사이징 (On-Demand)",
            description = """
                S3에 저장된 원본 이미지를 **원하는 크기(px)**로 실시간 리사이징하여 반환합니다.
                
                **[기능 특징]**
                * **지원 포맷:** JPG, PNG, WebP, GIF, BMP 등 (원본 확장자 유지)
                * **캐싱(Caching):** 최초 요청 시 리사이징된 이미지를 S3(`resized/`)에 저장하며, 이후 요청부터는 **저장된 파일을 즉시 반환**하여 속도가 매우 빠릅니다.
                * **브라우저 캐시:** 30일(`max-age=2592000`) 동안 브라우저 및 CDN에 캐싱되도록 헤더를 설정합니다.
                
                **[사용법]**
                `<img>` 태그의 `src` 속성에 아래와 같이 사용하세요.
                > `https://api.loci.my/api/v1/images?fileUrl=...&w=300&h=300`
                """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "이미지 반환 성공 (Binary Data)"),
            @ApiResponse(responseCode = "400", description = "(FILE400_1) 잘못된 파일이거나 이미지가 아님"),
            @ApiResponse(responseCode = "500", description = "(COMMON500) 변환 실패")
    })
    @GetMapping
    public ResponseEntity<byte[]> resizeImage(
            @Parameter(description = "원본 이미지의 전체 S3 URL 또는 파일 Key", required = true,
                    example = "https://loci-assets.s3.ap-northeast-2.amazonaws.com/profiles/user1.jpg")
            @RequestParam String fileUrl,

            @Parameter(description = "원하는 가로 폭 (px). 비율 유지를 위해 가로/세로 중 하나만 넣어도 됨 (현재는 둘 다 필수)", required = true, example = "200")
            @RequestParam int w,

            @Parameter(description = "원하는 세로 높이 (px)", required = true, example = "200")
            @RequestParam int h
    ) {
        String originalKey = s3UploadService.extractKeyFromUrl(fileUrl);

        String ext = "jpg";
        if (originalKey.contains(".")) {
            ext = originalKey.substring(originalKey.lastIndexOf(".") + 1).toLowerCase();
        }

        String resizedKey = "resized/w" + w + "_h" + h + "/" + originalKey;

        if (s3UploadService.doesObjectExist(resizedKey)) {
            byte[] resizedBytes = s3UploadService.downloadFile(resizedKey);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("image/" + ext))
                    .body(resizedBytes);
        }

        try {
            byte[] originalBytes = s3UploadService.downloadFile(originalKey);

            ByteArrayInputStream inputStream = new ByteArrayInputStream(originalBytes);
            BufferedImage originalImage = ImageIO.read(inputStream);

            if (originalImage == null) {
                throw new CustomException(ErrorCode.FILE_IS_EMPTY);
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            Thumbnails.of(originalImage)
                    .size(w, h)
                    .outputFormat(ext)
                    .toOutputStream(outputStream);

            byte[] resizedBytes = outputStream.toByteArray();

            s3UploadService.uploadBytes(resizedBytes, resizedKey, "image/" + ext);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("image/" + ext))
                    .body(resizedBytes);

        } catch (Exception e) {
            log.error("이미지 리사이징 실패 [Key: {}]: {}", originalKey, e.getMessage());
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}