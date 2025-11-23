package com.teamloci.loci.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.teamloci.loci.global.response.CustomResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Health", description = "서버 상태 확인 API")
@RestController
public class HealthController {

    @Operation(summary = "[Health] 1. 서버 상태 확인",
            description = "현재 서버가 정상적으로 실행 중인지 확인합니다. (ELB Health Check 용)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "서버 정상",
                    content = @Content(schema = @Schema(implementation = CustomResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "timestamp": "2025-11-04T21:00:00.123456",
                                      "isSuccess": true,
                                      "code": "COMMON200",
                                      "message": "성공적으로 요청을 수행했습니다.",
                                      "result": "ok"
                                    }
                                    """)))
    })
    @GetMapping("/health")
    public ResponseEntity<CustomResponse<String>> health() {
        return ResponseEntity.ok(CustomResponse.ok("ok"));
    }
}