package com.tchalanet.server.core.uslottery.domain.rules;

import com.tchalanet.server.core.uslottery.domain.model.DrawMain;

public final class UsLotteryDrawRules {
    private UsLotteryDrawRules() {}

    public static void validateMainSize(String channelCode, DrawMain main) {
        int expected = expectedSize(channelCode);
        if (expected > 0) {
            main.requireSize(expected, channelCode);
        }
    }

    private static int expectedSize(String channelCode) {
        if (channelCode == null) return 0;
        return switch (channelCode) {
            case "US_FL_NUM3_MID", "US_FL_NUM3_EVE", "US_NY_NUM3_MID", "US_NY_NUM3_EVE" -> 3;
            case "US_FL_NUM4_MID", "US_FL_NUM4_EVE", "US_NY_NUM4_MID", "US_NY_NUM4_EVE" -> 4;
            default -> 0; // unknown => no strict validation in v1
        };
    }
}
