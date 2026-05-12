package com.tchalanet.server.common.time;

import java.time.*;

public final class OccurredAtResolver {
    private OccurredAtResolver() {}

    public static Instant resolveOrThrow(
        Instant primary,
        LocalDate drawDate,
        LocalTime drawTime,
        ZoneId zone
    ) {
        if (primary != null) {
            return primary;
        }

        if (drawDate == null) {
            throw new IllegalArgumentException("drawDate is required to resolve occurredAt");
        }
        if (drawTime == null) {
            throw new IllegalArgumentException("drawTime is required to resolve occurredAt");
        }
        if (zone == null) {
            throw new IllegalArgumentException("zone is required to resolve occurredAt");
        }

        return ZonedDateTime.of(drawDate, drawTime, zone).toInstant();
    }

    public static Instant resolveOrNow(
        Instant primary,
        LocalDate drawDate,
        LocalTime drawTime,
        ZoneId zone,
        Clock clock
    ) {
        if (primary != null) {
            return primary;
        }

        if (drawDate != null && drawTime != null && zone != null) {
            return ZonedDateTime.of(drawDate, drawTime, zone).toInstant();
        }

        return clock.instant();
    }
}
