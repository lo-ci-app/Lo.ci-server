package com.teamloci.loci.controller;

import com.teamloci.loci.dto.PostDto;
import com.teamloci.loci.global.exception.CustomException;
import com.teamloci.loci.global.exception.code.ErrorCode;
import com.teamloci.loci.global.response.CustomResponse;
import com.teamloci.loci.global.security.AuthenticatedUser;
import com.teamloci.loci.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
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

@Tag(name = "Post", description = "포스트(게시물) API")
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
            description = "새로운 포스트를 생성합니다. (미디어, 공동 작업자 포함)")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "포스트 내용, 미디어 목록, 공동 작업자 ID 목록",
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
                      "collaboratorIds": [2, 3]
                    }
                    """))
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "포스트 생성 성공",
                    content = @Content(schema = @Schema(implementation = CustomResponse.class),
                            examples = @ExampleObject(value = """
                                    { "code": "COMMON201", "result": { "id": 1, "contents": "오늘의 기록", ... } }
                                    """))),
            @ApiResponse(responseCode = "401", description = "(COMMON401) 인증 실패", content = @Content),
            @ApiResponse(responseCode = "404", description = "(USER404_1) 공동 작업자 ID로 유저를 찾을 수 없음", content = @Content)
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
            description = "특정 포스트의 상세 정보를 조회합니다. (미디어, 공동 작업자 포함)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = CustomResponse.class),
                            examples = @ExampleObject(value = """
                                    { "code": "COMMON200", "result": { "id": 1, "contents": "오늘의 기록", ... } }
                                    """))),
            @ApiResponse(responseCode = "404", description = "(POST404_1) 포스트를 찾을 수 없음", content = @Content)
    })
    @GetMapping("/{postId}")
    public ResponseEntity<CustomResponse<PostDto.PostDetailResponse>> getPost(
            @PathVariable Long postId
    ) {
        PostDto.PostDetailResponse response = postService.getPost(postId);
        return ResponseEntity.ok(CustomResponse.ok(response));
    }

    @Operation(summary = "[Post] 3. 특정 유저의 포스트 목록 조회",
            description = "특정 사용자 ID(userId)가 작성한 포스트 목록을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공 (데이터가 없으면 빈 리스트 `[]` 반환)",
                    content = @Content(schema = @Schema(implementation = CustomResponse.class),
                            examples = @ExampleObject(value = """
                                    { "code": "COMMON200", "result": [ { "id": 1, ... }, { "id": 2, ... } ] }
                                    """)))
    })
    @GetMapping("/user/{userId}")
    public ResponseEntity<CustomResponse<List<PostDto.PostDetailResponse>>> getPostsByUser(
            @PathVariable Long userId
    ) {
        List<PostDto.PostDetailResponse> response = postService.getPostsByUser(userId);
        return ResponseEntity.ok(CustomResponse.ok(response));
    }

    @Operation(summary = "[Post] 4. 포스트 삭제",
            description = "특정 포스트를 삭제합니다. (작성자 본인만 가능)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "삭제 성공",
                    content = @Content(schema = @Schema(implementation = CustomResponse.class),
                            examples = @ExampleObject(value = """
                                    { "code": "COMMON200", "result": null }
                                    """))),
            @ApiResponse(responseCode = "401", description = "(COMMON401) 인증 실패", content = @Content),
            @ApiResponse(responseCode = "403", description = "(POST403_1) 포스트 작성자가 아님", content = @Content),
            @ApiResponse(responseCode = "404", description = "(POST404_1) 포스트를 찾을 수 없음", content = @Content)
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
            description = "특정 포스트의 내용을 수정합니다. (작성자 본인만 가능)")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "수정할 포스트 내용, 미디어 목록, 공동 작업자 ID 목록. (Post 1. 생성 DTO와 동일)",
            required = true,
            content = @Content(examples = @ExampleObject(value = """
                    {
                      "contents": "수정된 내용",
                      "mediaList": [
                        {
                          "mediaUrl": "https://fiv5-assets.s3.../new_image.png",
                          "mediaType": "IMAGE",
                          "sortOrder": 1
                        }
                      ],
                      "collaboratorIds": [2, 4]
                    }
                    """))
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "수정 성공",
                    content = @Content(schema = @Schema(implementation = CustomResponse.class),
                            examples = @ExampleObject(value = """
                                    { "code": "COMMON200", "result": { "id": 1, "contents": "수정된 내용", ... } }
                                    """))),
            @ApiResponse(responseCode = "401", description = "(COMMON401) 인증 실패", content = @Content),
            @ApiResponse(responseCode = "403", description = "(POST403_1) 포스트 작성자가 아님", content = @Content),
            @ApiResponse(responseCode = "404", description = "(POST404_1) 포스트를 찾을 수 없음 / (USER404_1) 공동 작업자 ID로 유저를 찾을 수 없음", content = @Content)
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
}