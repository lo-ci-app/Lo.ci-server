package com.teamloci.loci.controller;

import com.teamloci.loci.dto.GuestbookDto;
import com.teamloci.loci.global.exception.CustomException;
import com.teamloci.loci.global.exception.code.ErrorCode;
import com.teamloci.loci.global.response.CustomResponse;
import com.teamloci.loci.global.security.AuthenticatedUser;
import com.teamloci.loci.service.GuestbookService;
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

@Tag(name = "Guestbook (Timeline)", description = "방명록(타임라인) API")
@RestController
@RequestMapping("/api/v1/guestbook")
@RequiredArgsConstructor
public class GuestbookController {

    private final GuestbookService guestbookService;

    private Long getUserId(AuthenticatedUser user) {
        if (user == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
        return user.getUserId();
    }

    @Operation(summary = "[Guestbook] 1. 방명록 작성",
            description = "다른 사용자(hostId)의 방명록에 글을 작성합니다. (블루투스 근접 확인은 클라이언트가 담당)")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "방명록 내용",
            required = true,
            content = @Content(examples = @ExampleObject(value = "{\"contents\": \"만나서 반가워!\"}"))
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "작성 성공",
                    content = @Content(schema = @Schema(implementation = CustomResponse.class),
                            examples = @ExampleObject(value = """
                                    { "code": "COMMON201", "result": { "id": 1, "contents": "만나서 반가워!", ... } }
                                    """))),
            @ApiResponse(responseCode = "400", description = "(GUESTBOOK400_1) 자신의 방명록에 작성 시도", content = @Content),
            @ApiResponse(responseCode = "401", description = "(COMMON401) 인증 실패", content = @Content),
            @ApiResponse(responseCode = "404", description = "(USER404_1) 작성자 또는 호스트 유저를 찾을 수 없음", content = @Content)
    })
    @PostMapping("/{hostId}")
    public ResponseEntity<CustomResponse<GuestbookDto.GuestbookResponse>> createGuestbookEntry(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long hostId,
            @Valid @RequestBody GuestbookDto.GuestbookCreateRequest request
    ) {
        Long authorId = getUserId(user);
        GuestbookDto.GuestbookResponse response = guestbookService.createEntry(authorId, hostId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CustomResponse.created(response));
    }

    @Operation(summary = "[Guestbook] 2. 특정 유저의 방명록 목록 조회",
            description = "특정 사용자(hostId)의 방명록을 최신순으로 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공 (데이터가 없으면 빈 리스트 `[]` 반환)",
                    content = @Content(schema = @Schema(implementation = CustomResponse.class),
                            examples = @ExampleObject(value = """
                                    { "code": "COMMON200", "result": [ { "id": 1, "contents": "...", "author": { ... } }, ... ] }
                                    """)))
    })
    @GetMapping("/{hostId}")
    public ResponseEntity<CustomResponse<List<GuestbookDto.GuestbookResponse>>> getGuestbookEntries(
            @PathVariable Long hostId
    ) {
        List<GuestbookDto.GuestbookResponse> response = guestbookService.getGuestbook(hostId);
        return ResponseEntity.ok(CustomResponse.ok(response));
    }

    @Operation(summary = "[Guestbook] 3. 방명록 삭제",
            description = "특정 방명록 항목(entryId)을 삭제합니다. (방명록 주인 또는 작성자 본인만 가능)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "삭제 성공",
                    content = @Content(schema = @Schema(implementation = CustomResponse.class),
                            examples = @ExampleObject(value = """
                                    { "code": "COMMON200", "result": null }
                                    """))),
            @ApiResponse(responseCode = "401", description = "(COMMON401) 인증 실패", content = @Content),
            @ApiResponse(responseCode = "403", description = "(COMMON403) 방명록 주인이나 작성자가 아님", content = @Content),
            @ApiResponse(responseCode = "404", description = "(GUESTBOOK404_1) 방명록 항목을 찾을 수 없음", content = @Content)
    })
    @DeleteMapping("/{entryId}")
    public ResponseEntity<CustomResponse<Void>> deleteGuestbookEntry(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long entryId
    ) {
        Long currentUserId = getUserId(user);
        guestbookService.deleteEntry(currentUserId, entryId);
        return ResponseEntity.ok(CustomResponse.ok(null));
    }
}