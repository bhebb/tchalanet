package com.tchalanet.server.core.uslottery.internal.application.port.out;

import java.time.Instant;
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
    Set<String> externalGameCodes,
    String providerSlotCode,
    boolean force,
    boolean includeRaw,
    Instant requestedAt
) {

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

        externalGameCodes =
            externalGameCodes == null
                ? Set.of()
                : externalGameCodes.stream()
                  .filter(s -> s != null && !s.isBlank())
                  .map(s -> s.trim().toUpperCase(Locale.ROOT))
                  .collect(Collectors.toUnmodifiableSet());

        if (externalGameCodes.isEmpty()) {
            throw new IllegalArgumentException("externalGameCodes required");
        }
    }
}
