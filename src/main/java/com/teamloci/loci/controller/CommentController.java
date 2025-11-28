package com.teamloci.loci.controller;

import com.teamloci.loci.dto.CommentDto;
import com.teamloci.loci.global.exception.CustomException;
import com.teamloci.loci.global.exception.code.ErrorCode;
import com.teamloci.loci.global.response.CustomResponse;
import com.teamloci.loci.global.security.AuthenticatedUser;
import com.teamloci.loci.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Comment", description = "게시물 댓글 API")
@RestController
@RequestMapping("/api/v1/posts/{postId}/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    private Long getUserId(AuthenticatedUser user) {
        if (user == null) throw new CustomException(ErrorCode.UNAUTHORIZED);
        return user.getUserId();
    }

    @Operation(summary = "댓글 작성",
            description = """
                    특정 포스트에 댓글을 작성합니다.
                    
                    * **반환값:** 생성된 댓글 객체를 반환합니다. `user` 필드는 유저 조회 응답(`UserResponse`)과 동일한 구조를 가집니다.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "작성 성공",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "code": "COMMON200",
                              "message": "성공적으로 요청을 수행했습니다.",
                              "result": {
                                "id": 123,
                                "content": "사진 정말 잘 나왔네요!",
                                "user": {
                                  "id": 1,
                                  "handle": "happy_quokka",
                                  "nickname": "내닉네임",
                                  "profileUrl": "https://fiv5.../profiles/me.jpg",
                                  "createdAt": "2025-01-01T00:00:00",
                                  "relationStatus": "SELF",
                                  "friendCount": 0,
                                  "postCount": 0
                                },
                                "createdAt": "2025-11-25T12:34:56"
                              }
                            }
                            """))),
            @ApiResponse(responseCode = "404", description = "(POST404_1) 존재하지 않는 게시물")
    })
    @PostMapping
    public ResponseEntity<CustomResponse<CommentDto.Response>> createComment(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long postId,
            @Valid @RequestBody CommentDto.CreateRequest request
    ) {
        return ResponseEntity.ok(CustomResponse.ok(
                commentService.createComment(getUserId(user), postId, request)
        ));
    }

    @Operation(summary = "댓글 목록 조회 (무한 스크롤)",
            description = """
                    해당 포스트의 댓글을 최신순(ID 내림차순)으로 조회합니다.
                    
                    * **삭제 권한:** `user.relationStatus == 'SELF'` 인 경우 삭제 버튼을 노출하세요.
                    * **페이지네이션:** `hasNext`가 true면 `nextCursor` 값을 다음 API 호출 시 `cursorId` 파라미터로 보내세요.
                    * **전체 개수:** `totalCount` 필드를 통해 댓글 창 상단의 '댓글 (N)' 타이틀을 갱신하세요.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "code": "COMMON200",
                              "result": {
                                "comments": [
                                  {
                                    "id": 120,
                                    "content": "최신 댓글입니다.",
                                    "user": {
                                      "id": 5,
                                      "handle": "best_friend",
                                      "nickname": "친구1",
                                      "profileUrl": "https://fiv5.../profiles/friend.jpg",
                                      "createdAt": "2025-02-01T00:00:00",
                                      "relationStatus": "FRIEND",
                                      "friendCount": 0,
                                      "postCount": 0
                                    },
                                    "createdAt": "2025-11-25T12:30:00"
                                  }
                                ],
                                "hasNext": true,
                                "nextCursor": 115,
                                "totalCount": 42
                              }
                            }
                            """)))
    })
    @GetMapping
    public ResponseEntity<CustomResponse<CommentDto.ListResponse>> getComments(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long postId,
            @Parameter(description = "이전 페이지의 마지막 댓글 ID (첫 요청 시 null)") @RequestParam(required = false) Long cursorId,
            @Parameter(description = "가져올 개수") @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(CustomResponse.ok(
                commentService.getComments(getUserId(user), postId, cursorId, size)
        ));
    }

    @Operation(summary = "댓글 삭제",
            description = """
                    내가 쓴 댓글을 삭제합니다.
                    
                    * **권한:** 본인이 작성한 댓글(`relationStatus == SELF`)만 삭제 가능합니다.
                    * **에러:** 남의 댓글을 삭제하려고 하거나, 해당 게시물의 댓글이 아닌 경우 에러가 발생합니다.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "삭제 성공"),
            @ApiResponse(responseCode = "403", description = "권한 없음 (남의 댓글)"),
            @ApiResponse(responseCode = "404", description = "댓글을 찾을 수 없음"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (게시물 ID 불일치)")
    })
    @DeleteMapping("/{commentId}")
    public ResponseEntity<CustomResponse<Void>> deleteComment(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long postId,
            @PathVariable Long commentId
    ) {
        commentService.deleteComment(getUserId(user), postId, commentId);
        return ResponseEntity.ok(CustomResponse.ok(null));
    }
}