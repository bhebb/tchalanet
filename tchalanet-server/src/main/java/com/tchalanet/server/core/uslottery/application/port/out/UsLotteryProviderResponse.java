package com.tchalanet.server.core.uslottery.application.port.out;

import com.tchalanet.server.common.types.enums.UsLotteryProvider;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

public record UsLotteryProviderResponse(
    UsLotteryProvider provider,
    LocalDate drawDate,
    LocalTime drawTime,
    ZoneId timezone,
    List<UsLotteryProviderResult> results,
    Object rawPayload) {

    public static UsLotteryProviderResponse empty(UsLotteryProvider provider, UsLotteryProviderQuery query) {
        return new UsLotteryProviderResponse(
            provider,
            query.drawDate(),
            query.drawTime(),
            query.timezone(),
            List.of(),
            null);
    }

    public UsLotteryProviderResult findByGameCode(String gameCode) {
        if (gameCode == null || results == null) {
            return null;
        }

        return results.stream()
            .filter(r -> gameCode.equalsIgnoreCase(r.externalGameCode()))
            .findFirst()
            .orElse(null);
    }
}
