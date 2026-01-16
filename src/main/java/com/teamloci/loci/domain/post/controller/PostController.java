package com.teamloci.loci.domain.post.controller;

import java.util.List;

import com.teamloci.loci.domain.post.dto.PostDto;
import com.teamloci.loci.domain.post.dto.ReactionDto;
import com.teamloci.loci.domain.post.service.PostService;
import com.teamloci.loci.domain.post.service.ReactionService;
import com.teamloci.loci.domain.post.entity.ReactionType;
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

import com.teamloci.loci.global.error.CustomException;
import com.teamloci.loci.global.error.ErrorCode;
import com.teamloci.loci.global.common.CustomResponse;
import com.teamloci.loci.global.auth.AuthenticatedUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Post", description = "게시물, 지도 타임라인, 피드 API")
@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final ReactionService reactionService;

    private Long getUserId(AuthenticatedUser user) {
        if (user == null) throw new CustomException(ErrorCode.UNAUTHORIZED);
        return user.getUserId();
    }

    @Operation(summary = "포스트 생성",
            description = "새로운 포스트를 작성합니다. \n\n응답의 `user.relationStatus`는 작성자 본인이므로 항상 `SELF`입니다.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(examples = @ExampleObject(value = """
                    {
                      "mediaList": [
                        { 
                          "mediaUrl": "https://dagvorl6p9q6m.cloudfront.net/posts/uuid_image.webp", 
                          "mediaType": "IMAGE", 
                          "sortOrder": 1 
                        }
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
            @ApiResponse(responseCode = "201", description = "생성 성공")
    })
    @PostMapping
    public ResponseEntity<CustomResponse<PostDto.PostDetailResponse>> createPost(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody PostDto.PostCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CustomResponse.created(postService.createPost(getUserId(user), request)));
    }

    @Operation(summary = "포스트 상세 조회",
            description = "포스트 ID로 상세 정보를 조회합니다.")
    @GetMapping("/{postId}")
    public ResponseEntity<CustomResponse<PostDto.PostDetailResponse>> getPost(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long postId
    ) {
        Long myUserId = getUserId(user);
        return ResponseEntity.ok(CustomResponse.ok(postService.getPost(postId, myUserId)));
    }

    @Operation(summary = "내 포스트 목록 조회 (단축 URL)",
            description = "내 포스트 목록을 조회합니다. `/api/v1/posts/user/{내ID}`와 동일하게 동작합니다.")
    @GetMapping("/me")
    public ResponseEntity<CustomResponse<PostDto.FeedResponse>> getMyPosts(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Parameter(description = "이전 페이지의 마지막 포스트 ID (첫 요청 시 null)")
            @RequestParam(required = false) Long cursor,
            @Parameter(description = "가져올 개수") @RequestParam(defaultValue = "10") int size
    ) {
        Long myUserId = getUserId(user);
        return ResponseEntity.ok(CustomResponse.ok(postService.getPostsByUser(myUserId, myUserId, cursor, size)));
    }

    @Operation(summary = "내 보관함 조회 (무한 스크롤)",
            description = "보관된(ARCHIVED) 나의 게시물 목록을 조회합니다.")
    @GetMapping("/me/archived")
    public ResponseEntity<CustomResponse<PostDto.FeedResponse>> getMyArchivedPosts(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Parameter(description = "이전 페이지의 마지막 포스트 ID (첫 요청 시 null)")
            @RequestParam(required = false) Long cursor,
            @Parameter(description = "가져올 개수") @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(CustomResponse.ok(postService.getArchivedPosts(getUserId(user), cursor, size)));
    }

    @Operation(summary = "유저별 포스트 목록 (무한 스크롤)",
            description = """
                    특정 유저(targetUserId)가 작성한 포스트들을 최신순으로 조회합니다.
                    """)
    @GetMapping("/user/{userId}")
    public ResponseEntity<CustomResponse<PostDto.FeedResponse>> getPostsByUser(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long userId,
            @Parameter(description = "이전 페이지의 마지막 포스트 ID (첫 요청 시 null)")
            @RequestParam(required = false) Long cursor,
            @Parameter(description = "가져올 개수") @RequestParam(defaultValue = "10") int size
    ) {
        Long myUserId = getUserId(user);
        return ResponseEntity.ok(CustomResponse.ok(postService.getPostsByUser(myUserId, userId, cursor, size)));
    }

    @Operation(summary = "비콘(장소) 타임라인 조회", description = "특정 비콘의 게시글 목록을 조회합니다. (친구 공개 + 내 글) - 커서 기반 페이지네이션")
    @GetMapping("/beacon/{beaconId}")
    public ResponseEntity<CustomResponse<PostDto.FeedResponse>> getPostsByBeaconId(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Parameter(description = "비콘 ID", required = true) @PathVariable String beaconId,
            @Parameter(description = "마지막으로 조회한 게시글 ID (첫 조회 시 null)") @RequestParam(required = false) Long cursorId,
            @Parameter(description = "조회할 개수 (기본 10)") @RequestParam(defaultValue = "10") int size
    ) {
        PostDto.FeedResponse response = postService.getPostsByBeaconId(beaconId, user.getUserId(), cursorId, size);
        return ResponseEntity.ok(CustomResponse.ok(response));
    }

    @Operation(summary = "지도 마커 (범위 조회)",
            description = """
                    지도 화면 내의 마커 정보를 반환합니다. 
                    `thumbnailImageUrl`은 CloudFront 리사이징 URL(`w300` 등)로 제공될 수 있습니다.
                    
                    * **필터링:** 나와 내 친구의 'ACTIVE' 상태인 게시물만 집계합니다.
                    """)
    @GetMapping("/map")
    public ResponseEntity<CustomResponse<List<PostDto.MapMarkerResponse>>> getMapMarkers(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Parameter(description = "SW 위도") @RequestParam Double minLat,
            @Parameter(description = "NE 위도") @RequestParam Double maxLat,
            @Parameter(description = "SW 경도") @RequestParam Double minLon,
            @Parameter(description = "NE 경도") @RequestParam Double maxLon
    ) {
        return ResponseEntity.ok(CustomResponse.ok(postService.getMapMarkers(minLat, maxLat, minLon, maxLon, getUserId(user))));
    }

    @Operation(summary = "지도 마커 (Time Mode)",
            description = """
                    **Time 모드용 API**입니다.
                    * 지도 범위와 상관없이 **내 모든 친구들의 가장 최신 게시글 위치**를 반환합니다.
                    * 각 친구당 1개의 마커(게시글)만 반환되며, **User/Beacon/Post** 정보가 구조화되어 응답됩니다.
                    """)
    @GetMapping("/map/friends")
    public ResponseEntity<CustomResponse<List<PostDto.FriendMapMarkerResponse>>> getFriendMapMarkers(
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ResponseEntity.ok(CustomResponse.ok(postService.getFriendMapMarkers(getUserId(user))));
    }

    @Operation(summary = "친구 피드 (무한 스크롤)", description = "친구들의 최신 글을 모아봅니다. **ID 기반 커서**를 사용합니다.")
    @GetMapping("/feed")
    public ResponseEntity<CustomResponse<PostDto.FeedResponse>> getFriendFeed(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Parameter(description = "이전 페이지의 마지막 포스트 ID (첫 요청 시 null)")
            @RequestParam(required = false) Long cursor,
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

    @Operation(summary = "포스트 보관함으로 이동 (즉시)", description = "게시물을 즉시 보관함(ARCHIVED) 상태로 변경합니다.")
    @PatchMapping("/{postId}/archive")
    public ResponseEntity<CustomResponse<Void>> archivePost(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long postId
    ) {
        postService.archivePost(getUserId(user), postId);
        return ResponseEntity.ok(CustomResponse.ok(null));
    }

    @Operation(summary = "포스트 복구 (보관함 -> 피드)", description = "보관된 게시물을 다시 피드로 복구합니다. (자동 보관 설정이 꺼집니다)")
    @PatchMapping("/{postId}/unarchive")
    public ResponseEntity<CustomResponse<Void>> unarchivePost(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long postId
    ) {
        postService.unarchivePost(getUserId(user), postId);
        return ResponseEntity.ok(CustomResponse.ok(null));
    }

    @Operation(summary = "게시글 반응(이모지) 토글",
            description = """
                    게시글에 이모지 반응을 남기거나 취소/변경합니다.
                    * 이미 누른 이모지면 -> **취소**
                    * 다른 이모지면 -> **변경**
                    * 없으면 -> **생성**
                    """)
    @PostMapping("/{postId}/reactions")
    public ResponseEntity<CustomResponse<Void>> toggleReaction(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long postId,
            @Parameter(description = "이모지 타입 (LIKE, LOVE, FUNNY, SURPRISED, SAD, ANGRY)", required = true)
            @RequestParam ReactionType type
    ) {
        reactionService.togglePostReaction(getUserId(user), postId, type);
        return ResponseEntity.ok(CustomResponse.ok(null));
    }

    @Operation(summary = "게시글 반응(이모지) 목록 조회",
            description = """
                해당 게시물에 달린 반응 목록을 조회합니다.
                
                * **정렬:** **내가 누른 반응이 있다면 항상 리스트의 0번째 인덱스**에 위치합니다. 그 뒤로는 최신순입니다.
                * **User Info:** 각 반응 객체(`user`)에는 나와의 친구 관계, 친구 수 등의 정보가 모두 포함됩니다.
                """)
    @GetMapping("/{postId}/reactions")
    public ResponseEntity<CustomResponse<ReactionDto.ListResponse>> getReactions(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long postId,
            @Parameter(description = "이전 페이지의 마지막 리액션 ID") @RequestParam(required = false) Long cursorId,
            @Parameter(description = "가져올 개수") @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(CustomResponse.ok(
                reactionService.getReactions(getUserId(user), postId, cursorId, size)
        ));
    }

    @Operation(summary = "유저의 방문한 장소 목록 (Grid View용 탭)",
            description = "유저가 방문했던 구역(Beacon)들을 최신순으로 묶어서 반환합니다.")
    @GetMapping("/user/{userId}/visited-places")
    public ResponseEntity<CustomResponse<List<PostDto.VisitedPlaceResponse>>> getVisitedPlaces(
            @PathVariable Long userId
    ) {
        return ResponseEntity.ok(CustomResponse.ok(postService.getVisitedPlaces(userId)));
    }

    @Operation(summary = "내 방문한 장소 목록 (Grid View용 탭)",
            description = "내가 방문했던 구역(Beacon)들을 최신순으로 묶어서 반환합니다. (/user/{내ID}/.. 와 동일)")
    @GetMapping("/me/visited-places")
    public ResponseEntity<CustomResponse<List<PostDto.VisitedPlaceResponse>>> getMyVisitedPlaces(
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ResponseEntity.ok(CustomResponse.ok(
                postService.getVisitedPlaces(getUserId(user))
        ));
    }

    @Operation(summary = "촬영 전 친구 발자취 확인",
            description = """
                   현재 위치(위경도)를 보내면, 이곳에 방문했던 친구 정보를 반환합니다.
                   * **size**: 반환받을 친구(방문자)의 최대 수 (기본값: 3)
                   """)
    @GetMapping("/check-location")
    public ResponseEntity<CustomResponse<PostDto.FriendVisitResponse>> checkLocation(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @Parameter(description = "가져올 친구 수 (기본값 3)")
            @RequestParam(defaultValue = "3") int size
    ) {
        return ResponseEntity.ok(CustomResponse.ok(
                postService.checkFriendFootprints(getUserId(user), latitude, longitude, size)
        ));
    }

    @Operation(summary = "게시물 설명 수정", description = "게시물의 본문(설명)만 수정합니다.")
    @PatchMapping("/{postId}/description")
    public ResponseEntity<CustomResponse<PostDto.PostDetailResponse>> updateDescription(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long postId,
            @Valid @RequestBody PostDto.DescriptionUpdateRequest request
    ) {
        return ResponseEntity.ok(CustomResponse.ok(
                postService.updateDescription(getUserId(user), postId, request)
        ));
    }
}