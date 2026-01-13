package com.teamloci.loci.domain.badge;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BadgeResponse {
    private Long id;
    private String name;
    private String condition;
    private String description;
    private String imageUrl;
    private boolean isAcquired;
    private boolean isMain;
}