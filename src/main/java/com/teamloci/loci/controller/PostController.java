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

import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "Post", description = "포스트(게시물) 및 타임라인, 지도 API")
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
            description = "새로운 포스트를 생성합니다. 입력된 위치(위도, 경도)는 Uber H3 육각형 그리드 시스템(Res 9)을 통해 비콘 ID로 변환되어 저장됩니다. 'isAutoArchive'가 true(기본값)일 경우 30일 후 보관함으로 이동됩니다.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "포스트 생성 요청 정보",
            required = true,
            content = @Content(examples = @ExampleObject(value = """
                    {
                      "mediaList": [
                        {
                          "mediaUrl": "https://fiv5-assets.s3.ap-northeast-2.amazonaws.com/posts/image1.webp",
                          "mediaType": "IMAGE",
                          "sortOrder": 1
                        }
                      ],
                      "collaboratorIds": [2, 3],
                      "latitude": 37.5665,
                      "longitude": 126.9780,
                      "locationName": "서울시청",
                      "isAutoArchive": true
                    }
                    """))
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "포스트 생성 성공",
                    content = @Content(schema = @Schema(implementation = CustomResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패 (토큰 없음/만료)", content = @Content),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 사용자 (작성자 또는 공동작업자)", content = @Content)
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
            @ApiResponse(responseCode = "404", description = "존재하지 않는 포스트", content = @Content)
    })
    @GetMapping("/{postId}")
    public ResponseEntity<CustomResponse<PostDto.PostDetailResponse>> getPost(
            @PathVariable Long postId
    ) {
        PostDto.PostDetailResponse response = postService.getPost(postId);
        return ResponseEntity.ok(CustomResponse.ok(response));
    }

    @Operation(summary = "[Post] 3. 유저별 포스트 목록 조회",
            description = "특정 유저가 작성한 'ACTIVE' 상태의 모든 포스트를 최신순으로 조회합니다.")
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
            description = "자신이 작성한 포스트를 삭제합니다 (Hard Delete).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "삭제 성공",
                    content = @Content(schema = @Schema(implementation = CustomResponse.class))),
            @ApiResponse(responseCode = "403", description = "삭제 권한 없음 (작성자가 아님)", content = @Content),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 포스트", content = @Content)
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
            description = "포스트 내용 및 설정을 수정합니다. 위치 정보 변경 시 H3 비콘 ID가 재계산됩니다.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "수정할 포스트 정보",
            required = true,
            content = @Content(examples = @ExampleObject(value = """
                    {
                      "mediaList": [],
                      "collaboratorIds": [],
                      "latitude": 37.5665,
                      "longitude": 126.9780,
                      "locationName": "수정된 장소",
                      "isAutoArchive": false
                    }
                    """))
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "수정 성공",
                    content = @Content(schema = @Schema(implementation = CustomResponse.class))),
            @ApiResponse(responseCode = "403", description = "수정 권한 없음 (작성자가 아님)", content = @Content),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 포스트", content = @Content)
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
            description = "현재 사용자의 위치(위도, 경도)를 보내면, 해당 위치가 속한 H3 육각형 구역(Beacon) 내의 'ACTIVE' 포스트들을 최신순으로 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = CustomResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "code": "COMMON200",
                                      "result": [
                                        {
                                          "id": 1,
                                          "latitude": 37.5665,
                                          "longitude": 126.9780,
                                          "locationName": "서울광장",
                                          "author": { "id": 2, "nickname": "행복한쿼카", "profileUrl": "..." },
                                          "mediaList": [ { "id": 10, "mediaUrl": "...", "mediaType": "IMAGE", "sortOrder": 1 } ],
                                          "createdAt": "2025-11-20T10:00:00",
                                          "isAutoArchive": true
                                        }
                                      ]
                                    }
                                    """)))
    })
    @GetMapping("/timeline")
    public ResponseEntity<CustomResponse<List<PostDto.PostDetailResponse>>> getTimeline(
            @Parameter(description = "현재 위치 위도", required = true, example = "37.5665") @RequestParam Double latitude,
            @Parameter(description = "현재 위치 경도", required = true, example = "126.9780") @RequestParam Double longitude
    ) {
        List<PostDto.PostDetailResponse> response = postService.getPostsByLocation(latitude, longitude);
        return ResponseEntity.ok(CustomResponse.ok(response));
    }

    @Operation(summary = "[Post] 7. (Map) 지도 범위 내 마커(비콘) 조회",
            description = """
                현재 보고 있는 지도 화면의 **사각형 범위(Bounding Box)** 정보를 받아, 해당 범위 안에 있는 비콘(육각형)들의 요약 정보를 반환합니다.
                
                **[요청 파라미터 설명]**
                * `minLat`: 화면 **가장 아래쪽(남쪽)** 위도 (South-West Latitude)
                * `maxLat`: 화면 **가장 위쪽(북쪽)** 위도 (North-East Latitude)
                * `minLon`: 화면 **가장 왼쪽(서쪽)** 경도 (South-West Longitude)
                * `maxLon`: 화면 **가장 오른쪽(동쪽)** 경도 (North-East Longitude)
                """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = CustomResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "code": "COMMON200",
                                      "result": [
                                        {
                                          "beaconId": "89283082807ffff",
                                          "latitude": 37.5665,
                                          "longitude": 126.9780,
                                          "count": 5,
                                          "thumbnailImageUrl": "https://fiv5-assets.s3.../thumb.jpg"
                                        }
                                      ]
                                    }
                                    """)))
    })
    @GetMapping("/map")
    public ResponseEntity<CustomResponse<List<PostDto.MapMarkerResponse>>> getMapMarkers(
            @Parameter(description = "최소 위도 (SW Lat)", required = true, example = "37.5000") @RequestParam Double minLat,
            @Parameter(description = "최대 위도 (NE Lat)", required = true, example = "37.6000") @RequestParam Double maxLat,
            @Parameter(description = "최소 경도 (SW Lon)", required = true, example = "126.9000") @RequestParam Double minLon,
            @Parameter(description = "최대 경도 (NE Lon)", required = true, example = "127.0000") @RequestParam Double maxLon
    ) {
        List<PostDto.MapMarkerResponse> response = postService.getMapMarkers(minLat, maxLat, minLon, maxLon);
        return ResponseEntity.ok(CustomResponse.ok(response));
    }

    @Operation(summary = "[Post] 8. 친구 피드 조회 (무한 스크롤)",
            description = """
                내 친구들이 작성한 'ACTIVE' 상태의 포스트를 최신순으로 조회합니다. **커서 기반 페이지네이션**을 지원합니다.
                
                **[사용법]**
                * **첫 요청:** `cursor` 파라미터 없이 요청 -> 최신 글 `size`개 반환.
                * **다음 요청:** 응답 받은 `nextCursor` 값을 `cursor` 파라미터에 넣어서 요청.
                * `hasNext`가 `false`면 더 이상 글이 없는 것.
                """)
    @GetMapping("/feed")
    public ResponseEntity<CustomResponse<PostDto.FeedResponse>> getFriendFeed(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Parameter(description = "이전 페이지의 마지막 포스트 작성 시간 (첫 요청 시 생략)", example = "2025-11-20T10:00:00")
            @RequestParam(required = false) LocalDateTime cursor,
            @Parameter(description = "한 번에 가져올 개수 (기본값 10)", example = "10")
            @RequestParam(defaultValue = "10") int size
    ) {
        Long myUserId = getUserId(user);
        PostDto.FeedResponse response = postService.getFriendFeed(myUserId, cursor, size);
        return ResponseEntity.ok(CustomResponse.ok(response));
    }
}