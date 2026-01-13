package com.teamloci.loci.domain.version;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AppVersionService {

    private final AppVersionRepository appVersionRepository;

    @Transactional(readOnly = true)
    public VersionCheckResponse checkVersion(OsType osType, String currentVersion) {
        AppVersion appVersion = appVersionRepository.findTopByOsTypeOrderByIdDesc(osType)
                .orElseThrow(() -> new IllegalArgumentException("버전 정보가 존재하지 않습니다."));

        boolean isForceUpdate = compareVersion(currentVersion, appVersion.getMinVersion()) < 0;
        boolean isSoftUpdate = compareVersion(currentVersion, appVersion.getVersion()) < 0;

        return VersionCheckResponse.builder()
                .latestVersion(appVersion.getVersion())
                .forceUpdate(isForceUpdate)
                .softUpdate(!isForceUpdate && isSoftUpdate)
                .storeUrl(appVersion.getStoreUrl())
                .message(appVersion.getMessage())
                .build();
    }

    private int compareVersion(String v1, String v2) {
        String[] v1Parts = v1.split("\\.");
        String[] v2Parts = v2.split("\\.");
        int length = Math.max(v1Parts.length, v2Parts.length);

        for (int i = 0; i < length; i++) {
            int p1 = i < v1Parts.length ? Integer.parseInt(v1Parts[i]) : 0;
            int p2 = i < v2Parts.length ? Integer.parseInt(v2Parts[i]) : 0;
            if (p1 < p2) return -1;
            if (p1 > p2) return 1;
        }
        return 0;
    }
}