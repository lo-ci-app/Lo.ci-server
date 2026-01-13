package com.teamloci.loci.domain.version;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/versions")
@RequiredArgsConstructor
public class AppVersionController {

    private final AppVersionService appVersionService;

    @GetMapping
    public ResponseEntity<VersionCheckResponse> checkVersion(
            @RequestParam OsType osType,
            @RequestParam String version) {

        VersionCheckResponse response = appVersionService.checkVersion(osType, version);
        return ResponseEntity.ok(response);
    }
}