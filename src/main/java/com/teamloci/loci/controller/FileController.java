package com.teamloci.loci.controller;

import com.teamloci.loci.dto.FileDto;
import com.teamloci.loci.global.response.CustomResponse;
import com.teamloci.loci.service.S3UploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "File", description = "범용 파일 업로드 API")
@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController {

    private final S3UploadService s3UploadService;

    @Operation(summary = "범용 파일 업로드 (S3)",
            description = "파일(MultipartFile)과 저장할 디렉토리(directory)를 보내면 S3에 업로드하고, S3 URL을 즉시 반환합니다. (DB 저장 X)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "S3 업로드 성공",
                    content = @Content(schema = @Schema(implementation = CustomResponse.class),
                            examples = @ExampleObject(value = """
                             {
                               "code": "COMMON200",
                               "result": {
                                 "fileUrl": "https://fiv5-assets.s3.ap-northeast-2.amazonaws.com/profiles/uuid...image.jpg"
                               }
                             }
                             """))),
            @ApiResponse(responseCode = "400", description = "(FILE400_1) 업로드할 파일이 비어있음", content = @Content),
            @ApiResponse(responseCode = "500", description = "(S3500_1) S3 파일 업로드 실패", content = @Content)
    })
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CustomResponse<FileDto.FileUploadResponse>> uploadFile(

            @Parameter(description = "S3 내 저장 디렉토리 (예: profiles, posts)", required = true)
            @RequestParam("directory") String directory,

            @Schema(description = "업로드할 파일 (MultipartFile)", type = "string", format = "binary", required = true)
            @RequestPart("file") MultipartFile file
    ) {
        String fileUrl = s3UploadService.upload(file, directory);

        return ResponseEntity.ok(CustomResponse.ok(new FileDto.FileUploadResponse(fileUrl)));
    }
}