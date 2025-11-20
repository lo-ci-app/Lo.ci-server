package com.teamloci.loci.controller;

import com.teamloci.loci.dto.PostDto;
import com.teamloci.loci.global.exception.CustomException;
import com.teamloci.loci.global.exception.code.ErrorCode;
import com.teamloci.loci.global.response.CustomResponse;
import com.teamloci.loci.global.security.AuthenticatedUser;
import com.teamloci.loci.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Post", description = "포스트(게시물) 및 타임라인 API")
@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    private Long getUserId(AuthenticatedUser user) {
        if (user == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
        return user.getUserId();
    }

    @Operation(summary = "[Post] 1. 포스트 생성",
            description = "새로운 포스트를 생성합니다. 입력된 위치(위도, 경도)는 Uber H3 육각형 그리드 시스템(Res 9)을 통해 비콘 ID로 변환되어 저장됩니다.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "포스트 생성 요청 객체",
            required = true,
            content = @Content(examples = @ExampleObject(value = """
                    {
                      "contents": "오늘의 기록",
                      "mediaList": [
                        {
                          "mediaUrl": "https://fiv5-assets.s3.../image1.png",
                          "mediaType": "IMAGE",
                          "sortOrder": 1
                        }
                      ],
                      "collaboratorIds": [2, 3],
                      "latitude": 37.5665,
                      "longitude": 126.9780,
                      "locationName": "서울시청"
                    }
                    """))
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "포스트 생성 성공",
                    content = @Content(schema = @Schema(implementation = CustomResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 사용자", content = @Content)
    })
    @PostMapping
    public ResponseEntity<CustomResponse<PostDto.PostDetailResponse>> createPost(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody PostDto.PostCreateRequest request
    ) {
        Long authorId = getUserId(user);
        PostDto.PostDetailResponse response = postService.createPost(authorId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CustomResponse.created(response));
    }

    @Operation(summary = "[Post] 2. 포스트 상세 조회",
            description = "포스트 ID를 통해 상세 정보를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = CustomResponse.class))),
            @ApiResponse(responseCode = "404", description = "포스트를 찾을 수 없음", content = @Content)
    })
    @GetMapping("/{postId}")
    public ResponseEntity<CustomResponse<PostDto.PostDetailResponse>> getPost(
            @PathVariable Long postId
    ) {
        PostDto.PostDetailResponse response = postService.getPost(postId);
        return ResponseEntity.ok(CustomResponse.ok(response));
    }

    @Operation(summary = "[Post] 3. 유저별 포스트 목록 조회",
            description = "특정 유저가 작성한 모든 포스트를 최신순으로 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = CustomResponse.class)))
    })
    @GetMapping("/user/{userId}")
    public ResponseEntity<CustomResponse<List<PostDto.PostDetailResponse>>> getPostsByUser(
            @PathVariable Long userId
    ) {
        List<PostDto.PostDetailResponse> response = postService.getPostsByUser(userId);
        return ResponseEntity.ok(CustomResponse.ok(response));
    }

    @Operation(summary = "[Post] 4. 포스트 삭제",
            description = "자신이 작성한 포스트를 삭제합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "삭제 성공",
                    content = @Content(schema = @Schema(implementation = CustomResponse.class))),
            @ApiResponse(responseCode = "403", description = "삭제 권한 없음 (작성자가 아님)", content = @Content),
            @ApiResponse(responseCode = "404", description = "포스트를 찾을 수 없음", content = @Content)
    })
    @DeleteMapping("/{postId}")
    public ResponseEntity<CustomResponse<Void>> deletePost(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long postId
    ) {
        Long currentUserId = getUserId(user);
        postService.deletePost(currentUserId, postId);
        return ResponseEntity.ok(CustomResponse.ok(null));
    }

    @Operation(summary = "[Post] 5. 포스트 수정",
            description = "포스트 내용을 수정합니다. 위치 정보가 변경될 경우 H3 비콘 ID도 재계산됩니다.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "수정할 포스트 정보",
            required = true,
            content = @Content(examples = @ExampleObject(value = """
                    {
                      "contents": "수정된 내용",
                      "mediaList": [],
                      "collaboratorIds": [],
                      "latitude": 37.5665,
                      "longitude": 126.9780,
                      "locationName": "수정된 장소"
                    }
                    """))
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "수정 성공",
                    content = @Content(schema = @Schema(implementation = CustomResponse.class))),
            @ApiResponse(responseCode = "403", description = "수정 권한 없음", content = @Content),
            @ApiResponse(responseCode = "404", description = "포스트 없음", content = @Content)
    })
    @PatchMapping("/{postId}")
    public ResponseEntity<CustomResponse<PostDto.PostDetailResponse>> updatePost(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long postId,
            @Valid @RequestBody PostDto.PostCreateRequest request
    ) {
        Long currentUserId = getUserId(user);
        PostDto.PostDetailResponse response = postService.updatePost(currentUserId, postId, request);
        return ResponseEntity.ok(CustomResponse.ok(response));
    }

    @Operation(summary = "[Post] 6. 위치 기반 타임라인 조회 (Beacon)",
            description = "현재 위치(위도, 경도)를 보내면, 해당 위치가 속한 H3 육각형 구역(Beacon)의 포스트들을 최신순으로 반환합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = CustomResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "code": "COMMON200",
                                      "result": [
                                        {
                                          "id": 1,
                                          "contents": "여기 핫플이네!",
                                          "latitude": 37.5665,
                                          "longitude": 126.9780,
                                          "locationName": "서울광장",
                                          "author": { "id": 2, "nickname": "행복한쿼카", ... },
                                          "createdAt": "2025-11-20T10:00:00"
                                        }
                                      ]
                                    }
                                    """)))
    })
    @GetMapping("/timeline")
    public ResponseEntity<CustomResponse<List<PostDto.PostDetailResponse>>> getTimeline(
            @Parameter(description = "위도 (예: 37.5665)", required = true, example = "37.5665") @RequestParam Double latitude,
            @Parameter(description = "경도 (예: 126.9780)", required = true, example = "126.9780") @RequestParam Double longitude
    ) {
        List<PostDto.PostDetailResponse> response = postService.getPostsByLocation(latitude, longitude);
        return ResponseEntity.ok(CustomResponse.ok(response));
    }
}