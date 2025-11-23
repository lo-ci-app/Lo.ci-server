package com.teamloci.loci.controller;

import com.teamloci.loci.dto.FileDto;
import com.teamloci.loci.global.response.CustomResponse;
import com.teamloci.loci.service.S3UploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "File", description = "파일 업로드 유틸리티 (S3)")
@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController {

    private final S3UploadService s3UploadService;

    @Operation(summary = "일반 파일 업로드", description = "S3의 특정 디렉토리에 파일을 업로드하고 URL을 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "S3 업로드 성공"),
            @ApiResponse(responseCode = "400", description = "(FILE400_1) 업로드할 파일이 비어있음", content = @Content),
            @ApiResponse(responseCode = "500", description = "(S3500_1) S3 파일 업로드 실패", content = @Content)
    })
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CustomResponse<FileDto.FileUploadResponse>> uploadFile(
            @Parameter(description = "저장할 디렉토리명 (예: posts, misc)", required = true) @RequestParam("directory") String directory,
            @RequestPart("file") MultipartFile file
    ) {
        String fileUrl = s3UploadService.upload(file, directory);
        return ResponseEntity.ok(CustomResponse.ok(new FileDto.FileUploadResponse(fileUrl)));
    }
}