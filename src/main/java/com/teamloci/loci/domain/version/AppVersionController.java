package com.teamloci.loci.domain.version;

import com.teamloci.loci.global.common.CustomResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/versions")
@RequiredArgsConstructor
public class AppVersionController {

    private final AppVersionService appVersionService;

    @GetMapping
    public CustomResponse<VersionCheckResponse> checkVersion(
            @RequestParam OsType osType,
            @RequestParam String version) {

        VersionCheckResponse response = appVersionService.checkVersion(osType, version);

        return CustomResponse.ok(response);
    }
}