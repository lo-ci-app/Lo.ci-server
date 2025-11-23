package com.teamloci.loci.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

@Tag(name = "Post", description = "게시물(Loci) 및 지도/타임라인 API")
@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    private Long getUserId(AuthenticatedUser user) {
        if (user == null) throw new CustomException(ErrorCode.UNAUTHORIZED);
        return user.getUserId();
    }

    @Operation(summary = "[Post] 1. 포스트 생성",
            description = "새로운 포스트를 작성합니다. `isArchived`가 `true`(기본값)이면 30일 후 자동으로 보관함으로 이동합니다.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(examples = @ExampleObject(value = """
                    {
                      "mediaList": [
                        { "mediaUrl": "https://s3.../img.webp", "mediaType": "IMAGE", "sortOrder": 1 }
                      ],
                      "collaboratorIds": [2, 3],
                      "latitude": 37.5665,
                      "longitude": 126.9780,
                      "locationName": "서울시청",
                      "isArchived": true
                    }
                    """))
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "생성 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 사용자(공동작업자)")
    })
    @PostMapping
    public ResponseEntity<CustomResponse<PostDto.PostDetailResponse>> createPost(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody PostDto.PostCreateRequest request
    ) {
        Long authorId = getUserId(user);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CustomResponse.created(postService.createPost(authorId, request)));
    }

    @Operation(summary = "[Post] 2. 포스트 상세 조회", description = "ID로 포스트 상세 정보를 조회합니다.")
    @GetMapping("/{postId}")
    public ResponseEntity<CustomResponse<PostDto.PostDetailResponse>> getPost(@PathVariable Long postId) {
        return ResponseEntity.ok(CustomResponse.ok(postService.getPost(postId)));
    }

    @Operation(summary = "[Post] 3. 유저별 포스트 목록", description = "특정 유저의 'ACTIVE' 상태인 포스트 목록을 최신순으로 조회합니다.")
    @GetMapping("/user/{userId}")
    public ResponseEntity<CustomResponse<List<PostDto.PostDetailResponse>>> getPostsByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(CustomResponse.ok(postService.getPostsByUser(userId)));
    }

    @Operation(summary = "[Post] 4. 포스트 삭제", description = "포스트를 영구 삭제합니다.")
    @DeleteMapping("/{postId}")
    public ResponseEntity<CustomResponse<Void>> deletePost(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long postId
    ) {
        postService.deletePost(getUserId(user), postId);
        return ResponseEntity.ok(CustomResponse.ok(null));
    }

    @Operation(summary = "[Post] 5. 포스트 수정", description = "포스트 내용 및 보관 설정(`isArchived`)을 수정합니다.")
    @PatchMapping("/{postId}")
    public ResponseEntity<CustomResponse<PostDto.PostDetailResponse>> updatePost(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long postId,
            @Valid @RequestBody PostDto.PostCreateRequest request
    ) {
        return ResponseEntity.ok(CustomResponse.ok(postService.updatePost(getUserId(user), postId, request)));
    }

    @Operation(summary = "[Post] 6. (Timeline) 비콘 기반 타임라인 조회",
            description = """
                지도에서 마커를 클릭했을 때, 해당 **비콘(Beacon ID)에 속한 포스트 목록**을 조회합니다.
                `GET /map` API에서 얻은 `beaconId`를 그대로 전달하면 됩니다.
                """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = CustomResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "code": "COMMON200",
                                      "result": [
                                        { "id": 1, "beaconId": "89283082807ffff", "mediaList": [...], ... }
                                      ]
                                    }
                                    """)))
    })
    @GetMapping("/timeline")
    public ResponseEntity<CustomResponse<List<PostDto.PostDetailResponse>>> getTimeline(
            @Parameter(description = "조회할 비콘 ID (H3 Index)", required = true, example = "89283082807ffff") 
            @RequestParam String beaconId
    ) {
        List<PostDto.PostDetailResponse> response = postService.getPostsByBeaconId(beaconId);
        return ResponseEntity.ok(CustomResponse.ok(response));
    }

    @Operation(summary = "[Post] 7. (Map) 지도 범위 내 마커 조회",
            description = "현재 지도 화면 범위(Bounding Box) 내에 있는 비콘들의 요약 정보(개수, 썸네일)를 반환합니다.")
    @GetMapping("/map")
    public ResponseEntity<CustomResponse<List<PostDto.MapMarkerResponse>>> getMapMarkers(
            @Parameter(description = "SW Lat") @RequestParam Double minLat,
            @Parameter(description = "NE Lat") @RequestParam Double maxLat,
            @Parameter(description = "SW Lon") @RequestParam Double minLon,
            @Parameter(description = "NE Lon") @RequestParam Double maxLon
    ) {
        return ResponseEntity.ok(CustomResponse.ok(postService.getMapMarkers(minLat, maxLat, minLon, maxLon)));
    }

    @Operation(summary = "[Post] 8. 친구 피드 조회 (무한 스크롤)",
            description = "내 친구들의 최신 글을 조회합니다. `nextCursor`를 이용해 다음 페이지를 요청하세요.")
    @GetMapping("/feed")
    public ResponseEntity<CustomResponse<PostDto.FeedResponse>> getFriendFeed(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required = false) LocalDateTime cursor,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(CustomResponse.ok(postService.getFriendFeed(getUserId(user), cursor, size)));
    }
}