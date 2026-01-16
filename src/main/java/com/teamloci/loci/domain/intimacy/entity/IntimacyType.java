package com.teamloci.loci.domain.intimacy.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum IntimacyType {
    FRIEND_MADE(50, "친구 맺기"),
    REACTION(5, "반응 남기기"),
    COMMENT(10, "댓글 달기"),
    VISIT(30, "발자취 방문"),
    COLLABORATOR(50, "공동 작업"),
    NUDGE(1, "콕 찌르기");

    private final int point;
    private final String description;
}