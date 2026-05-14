package com.tchalanet.server.common.time;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public final class OccurredAtResolver {

    private OccurredAtResolver() {}

    public static Instant resolveOrThrow(
        Instant primary,
        LocalDate date,
        LocalTime time,
        ZoneId zone
    ) {
        if (primary != null) {
            return primary;
        }

        if (date == null) {
            throw new IllegalArgumentException("date is required to resolve occurredAt");
        }
        if (time == null) {
            throw new IllegalArgumentException("time is required to resolve occurredAt");
        }
        if (zone == null) {
            throw new IllegalArgumentException("zone is required to resolve occurredAt");
        }

        return ZonedDateTime.of(date, time, zone).toInstant();
    }

    public static Instant resolveOrNow(
        Instant primary,
        LocalDate date,
        LocalTime time,
        ZoneId zone,
        Clock clock
    ) {
        if (primary != null) {
            return primary;
        }

        if (date != null && time != null && zone != null) {
            return ZonedDateTime.of(date, time, zone).toInstant();
        }

        return clock.instant();
    }
}
