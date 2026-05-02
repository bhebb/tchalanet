package com.tchalanet.server.core.uslottery.application.port.out;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public record UsLotteryProviderQuery(
    LocalDate drawDate,
    LocalTime drawTime,
    ZoneId timezone,
    Set<String> gameCodes,
    boolean force,
    boolean includeRaw) {

    public UsLotteryProviderQuery {
        if (drawDate == null) {
            throw new IllegalArgumentException("drawDate is required");
        }
        if (drawTime == null) {
            throw new IllegalArgumentException("drawTime is required");
        }
        if (timezone == null) {
            throw new IllegalArgumentException("timezone is required");
        }

        gameCodes =
            gameCodes == null
                ? Set.of()
                : gameCodes.stream()
                  .filter(s -> s != null && !s.isBlank())
                  .map(s -> s.trim().toUpperCase(Locale.ROOT))
                  .collect(Collectors.toUnmodifiableSet());

        if (gameCodes.isEmpty()) {
            throw new IllegalArgumentException("gameCodes required");
        }
    }
}
