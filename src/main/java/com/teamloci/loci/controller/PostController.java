package com.teamloci.loci.controller;

import java.time.LocalDateTime;
import java.util.List;
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

@Tag(name = "Post", description = "게시물, 지도 타임라인, 피드 API")
@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    private Long getUserId(AuthenticatedUser user) {
        if (user == null) throw new CustomException(ErrorCode.UNAUTHORIZED);
        return user.getUserId();
    }

    @Operation(summary = "포스트 생성", description = "새로운 포스트를 작성합니다.")
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
            @ApiResponse(responseCode = "201", description = "생성 성공",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "timestamp": "2025-11-23T10:00:00",
                              "isSuccess": true,
                              "code": "COMMON201",
                              "message": "성공적으로 객체를 생성했습니다.",
                              "result": {
                                "id": 101,
                                "beaconId": "89283082807ffff",
                                "latitude": 37.5665,
                                "longitude": 126.9780,
                                "locationName": "서울시청",
                                "author": { "id": 1, "nickname": "내닉네임", "profileUrl": "https://s3.../me.jpg" },
                                "mediaList": [{ "id": 50, "mediaUrl": "https://s3.../img.jpg", "mediaType": "IMAGE", "sortOrder": 1 }],
                                "collaborators": [],
                                "createdAt": "2025-11-23T10:00:00",
                                "updatedAt": "2025-11-23T10:00:00",
                                "isArchived": false
                              }
                            }
                            """)))
    })
    @PostMapping
    public ResponseEntity<CustomResponse<PostDto.PostDetailResponse>> createPost(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody PostDto.PostCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CustomResponse.created(postService.createPost(getUserId(user), request)));
    }

    @Operation(summary = "포스트 상세 조회", description = "포스트 ID로 상세 정보를 조회합니다.")
    @GetMapping("/{postId}")
    public ResponseEntity<CustomResponse<PostDto.PostDetailResponse>> getPost(@PathVariable Long postId) {
        return ResponseEntity.ok(CustomResponse.ok(postService.getPost(postId)));
    }

    @Operation(summary = "유저별 포스트 목록",
            description = "특정 유저(userId)가 작성한 포스트들을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "timestamp": "2025-11-23T10:05:00",
                              "isSuccess": true,
                              "code": "COMMON200",
                              "message": "성공적으로 요청을 수행했습니다.",
                              "result": [
                                {
                                  "id": 105,
                                  "beaconId": "89283082807ffff",
                                  "latitude": 37.5665,
                                  "longitude": 126.9780,
                                  "locationName": "한강공원",
                                  "author": { "id": 5, "nickname": "즐거운판다", "profileUrl": "https://s3.../panda.jpg" },
                                  "mediaList": [],
                                  "createdAt": "2025-11-22T18:00:00",
                                  "isArchived": false
                                },
                                {
                                  "id": 98,
                                  "beaconId": "8928308280bffff",
                                  "latitude": 37.5700,
                                  "longitude": 126.9800,
                                  "locationName": "롯데타워",
                                  "author": { "id": 5, "nickname": "즐거운판다", "profileUrl": "https://s3.../panda.jpg" },
                                  "mediaList": [],
                                  "createdAt": "2025-11-20T14:30:00",
                                  "isArchived": true
                                }
                              ]
                            }
                            """)))
    })
    @GetMapping("/user/{userId}")
    public ResponseEntity<CustomResponse<List<PostDto.PostDetailResponse>>> getPostsByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(CustomResponse.ok(postService.getPostsByUser(userId)));
    }

    @Operation(summary = "타임라인 (비콘별 조회)",
            description = "지도에서 마커를 클릭했을 때, 해당 구역(Beacon)에 있는 포스트들을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "timestamp": "2025-11-23T10:10:00",
                              "isSuccess": true,
                              "code": "COMMON200",
                              "message": "성공적으로 요청을 수행했습니다.",
                              "result": [
                                {
                                  "id": 201,
                                  "beaconId": "89283082807ffff",
                                  "latitude": 37.5665,
                                  "longitude": 126.9780,
                                  "locationName": "강남역 11번 출구",
                                  "author": { "id": 10, "nickname": "강남토박이", "profileUrl": null },
                                  "createdAt": "2025-11-23T09:00:00",
                                  "isArchived": false
                                },
                                {
                                  "id": 202,
                                  "beaconId": "89283082807ffff",
                                  "latitude": 37.5665,
                                  "longitude": 126.9780,
                                  "locationName": "강남역",
                                  "author": { "id": 3, "nickname": "지나가던사람", "profileUrl": null },
                                  "createdAt": "2025-11-23T08:50:00",
                                  "isArchived": false
                                }
                              ]
                            }
                            """)))
    })
    @GetMapping("/timeline")
    public ResponseEntity<CustomResponse<List<PostDto.PostDetailResponse>>> getTimeline(
            @Parameter(description = "조회할 비콘 ID (H3 Index)", required = true, example = "89283082807ffff")
            @RequestParam String beaconId
    ) {
        return ResponseEntity.ok(CustomResponse.ok(postService.getPostsByBeaconId(beaconId)));
    }

    @Operation(summary = "지도 마커 (범위 조회)", description = "지도 화면 내의 마커 정보를 반환합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "timestamp": "2025-11-23T10:15:00",
                              "isSuccess": true,
                              "code": "COMMON200",
                              "message": "성공적으로 요청을 수행했습니다.",
                              "result": [
                                {
                                  "beaconId": "89283082807ffff",
                                  "latitude": 37.5665,
                                  "longitude": 126.9780,
                                  "count": 5,
                                  "thumbnailImageUrl": "https://s3.../thumb1.jpg"
                                },
                                {
                                  "beaconId": "8928308280bffff",
                                  "latitude": 37.5700,
                                  "longitude": 126.9800,
                                  "count": 2,
                                  "thumbnailImageUrl": "https://s3.../thumb2.jpg"
                                }
                              ]
                            }
                            """)))
    })
    @GetMapping("/map")
    public ResponseEntity<CustomResponse<List<PostDto.MapMarkerResponse>>> getMapMarkers(
            @Parameter(description = "SW 위도") @RequestParam Double minLat,
            @Parameter(description = "NE 위도") @RequestParam Double maxLat,
            @Parameter(description = "SW 경도") @RequestParam Double minLon,
            @Parameter(description = "NE 경도") @RequestParam Double maxLon
    ) {
        return ResponseEntity.ok(CustomResponse.ok(postService.getMapMarkers(minLat, maxLat, minLon, maxLon)));
    }

    @Operation(summary = "친구 피드 (무한 스크롤)", description = "친구들의 최신 글을 모아봅니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "timestamp": "2025-11-23T10:20:00",
                              "isSuccess": true,
                              "code": "COMMON200",
                              "message": "성공적으로 요청을 수행했습니다.",
                              "result": {
                                "posts": [
                                  {
                                    "id": 305,
                                    "beaconId": "89283082807ffff",
                                    "latitude": 37.5665,
                                    "longitude": 126.9780,
                                    "locationName": "친구네 집",
                                    "author": { "id": 7, "nickname": "절친1", "profileUrl": "..." },
                                    "createdAt": "2025-11-23T09:30:00",
                                    "isArchived": false
                                  }
                                ],
                                "hasNext": true,
                                "nextCursor": "2025-11-23T09:30:00"
                              }
                            }
                            """)))
    })
    @GetMapping("/feed")
    public ResponseEntity<CustomResponse<PostDto.FeedResponse>> getFriendFeed(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Parameter(description = "이전 페이지의 마지막 포스트 작성 시간 (첫 요청 시 null)", example = "2024-11-23T12:00:00")
            @RequestParam(required = false) LocalDateTime cursor,
            @Parameter(description = "가져올 개수") @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(CustomResponse.ok(postService.getFriendFeed(getUserId(user), cursor, size)));
    }

    @Operation(summary = "포스트 삭제", description = "포스트를 삭제합니다.")
    @DeleteMapping("/{postId}")
    public ResponseEntity<CustomResponse<Void>> deletePost(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long postId
    ) {
        postService.deletePost(getUserId(user), postId);
        return ResponseEntity.ok(CustomResponse.ok(null));
    }

    @Operation(summary = "포스트 수정", description = "포스트를 수정합니다.")
    @PatchMapping("/{postId}")
    public ResponseEntity<CustomResponse<PostDto.PostDetailResponse>> updatePost(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long postId,
            @Valid @RequestBody PostDto.PostCreateRequest request
    ) {
        return ResponseEntity.ok(CustomResponse.ok(postService.updatePost(getUserId(user), postId, request)));
    }
}