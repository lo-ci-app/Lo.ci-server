package com.teamloci.loci.domain.intimacy.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class IntimacyLevelUpEvent {
    private Long actorId;
    private Long targetId;
    private int newLevel;
}