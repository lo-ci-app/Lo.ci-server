package com.teamloci.loci.domain.intimacy.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum IntimacyLevel {
    LEVEL_1(1, 0),
    LEVEL_2(2, 50),
    LEVEL_3(3, 150),
    LEVEL_4(4, 300);

    private final int level;
    private final long requiredTotalScore;

    private static final IntimacyLevel[] CACHED_VALUES = values();

    public static int calculateLevel(long currentScore) {
        IntimacyLevel[] levels = values();
        for (int i = levels.length - 1; i >= 0; i--) {
            if (currentScore >= levels[i].requiredTotalScore) {
                return levels[i].level;
            }
        }
        return 1;
    }
}