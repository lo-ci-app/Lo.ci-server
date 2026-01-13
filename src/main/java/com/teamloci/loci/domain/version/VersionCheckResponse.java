package com.teamloci.loci.domain.version;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class VersionCheckResponse {
    private String latestVersion;
    private boolean forceUpdate;
    private boolean softUpdate;
    private String storeUrl;
    private String message;
}