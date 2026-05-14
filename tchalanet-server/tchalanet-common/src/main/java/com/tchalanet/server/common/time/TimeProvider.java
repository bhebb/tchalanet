package com.tchalanet.server.common.time;

import com.tchalanet.server.common.context.TchRequestContext;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Component
public class TimeProvider {

    private static final ZoneId UTC = ZoneId.of("UTC");

    private final Clock clock;

    public TimeProvider(Clock clock) {
        this.clock = clock;
    }

    public Instant nowInstant() {
        return clock.instant();
    }

    public ZonedDateTime now(ZoneId zone) {
        return ZonedDateTime.ofInstant(clock.instant(), zoneOrUtc(zone));
    }

    public LocalDate today(ZoneId zone) {
        return now(zone).toLocalDate();
    }

    public ZonedDateTime now(TchRequestContext ctx) {
        return now(ctx == null ? null : ctx.tenantZoneId());
    }

    public LocalDate today(TchRequestContext ctx) {
        return today(ctx == null ? null : ctx.tenantZoneId());
    }

    /**
     * Legacy/helper conversion for ambiguous LocalDateTime values.
     *
     * <p>Prefer Instant for persisted business events.
     */
    public Instant toInstant(LocalDateTime ldt, ZoneId zone) {
        if (ldt == null) {
            return null;
        }
        return ZonedDateTime.of(ldt, zoneOrUtc(zone)).toInstant();
    }

    /**
     * Legacy/helper conversion using tenant timezone when available.
     *
     * <p>Prefer Instant for persisted business events.
     */
    public Instant toInstant(LocalDateTime ldt, TchRequestContext ctx) {
        if (ldt == null) {
            return null;
        }
        return toInstant(ldt, ctx == null ? null : ctx.tenantZoneId());
    }

    private static ZoneId zoneOrUtc(ZoneId zone) {
        return zone == null ? UTC : zone;
    }
}
