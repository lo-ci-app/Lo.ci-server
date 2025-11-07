package com.teamfiv5.fiv5.global.util;

import java.util.List;
import java.util.Random;

public class RandomNicknameGenerator {

    private static final List<String> ADJECTIVES = List.of(
            "행복한", "즐거운", "용감한", "수줍은", "똑똑한", "친절한", "배고픈", "졸린"
    );

    private static final List<String> NOUNS = List.of(
            "강아지", "고양이", "호랑이", "사자", "코끼리", "기린", "원숭이", "판다", "쿼카"
    );

    private static final Random RANDOM = new Random();

    public static String generate() {
        String adjective = ADJECTIVES.get(RANDOM.nextInt(ADJECTIVES.size()));
        String noun = NOUNS.get(RANDOM.nextInt(NOUNS.size()));
        return adjective + " " + noun;
    }
}