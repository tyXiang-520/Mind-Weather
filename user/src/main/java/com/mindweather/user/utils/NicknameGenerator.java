package com.mindweather.user.utils;

import java.util.concurrent.ThreadLocalRandom;

public class NicknameGenerator {

    private NicknameGenerator() {}

    public static String generateNickname() {
        int randomNum = ThreadLocalRandom.current().nextInt(10000, 99999);
        return "user_" + randomNum;
    }
}
