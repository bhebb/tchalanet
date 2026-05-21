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
public class TimeProvider implements TchTimeProvider {

    private static final ZoneId UTC = ZoneId.of("UTC");

    private final Clock clock;

    public TimeProvider(Clock clock) {
        this.clock = clock;
    }

    /** {@inheritDoc} */
    @Override
    public Instant now() {
        return clock.instant();
    }

    /**
     * Legacy alias for {@link #now()}.
     *
     * @deprecated use {@link #now()} instead
     */
    @Deprecated(forRemoval = false)
    public Instant nowInstant() {
        return now();
    }

    /** {@inheritDoc} */
    @Override
    public ZonedDateTime nowAt(ZoneId zoneId) {
        return ZonedDateTime.ofInstant(clock.instant(), zoneOrUtc(zoneId));
    }

    /** {@inheritDoc} */
    @Override
    public LocalDate today(ZoneId zone) {
        return nowAt(zone).toLocalDate();
    }

    /** {@inheritDoc} */
    @Override
    public Clock clock() {
        return clock;
    }

    public ZonedDateTime now(ZoneId zone) {
        return nowAt(zone);
    }

    public ZonedDateTime now(TchRequestContext ctx) {
        return nowAt(ctx == null ? null : ctx.tenantZoneId());
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
