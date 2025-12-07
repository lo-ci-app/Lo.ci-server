package com.teamloci.loci.domain.intimacy.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum IntimacyLevel {
    LV1(1, 0),
    LV2(2, 100),
    LV3(3, 220),
    LV4(4, 376),
    LV5(5, 594),
    LV6(6, 922),
    LV7(7, 1413),
    LV8(8, 2175),
    LV9(9, 3393),
    LV10(10, 5404);

    private final int level;
    private final int requiredTotalScore;

    public static int calculateLevel(long currentScore) {
        return Arrays.stream(values())
                .filter(l -> currentScore >= l.requiredTotalScore)
                .mapToInt(IntimacyLevel::getLevel)
                .max()
                .orElse(1);
    }
}