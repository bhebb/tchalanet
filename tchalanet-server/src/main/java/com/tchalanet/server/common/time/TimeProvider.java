package com.tchalanet.server.common.time;

import com.tchalanet.server.common.context.TchRequestContext;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Provides current time operations with timezone awareness.
 *
 * Uses Clock for testability (can be frozen in tests).
 * Supports explicit ZoneId or derives from TchRequestContext.
 */
@Component
public class TimeProvider {

    private final Clock clock;

    public TimeProvider(Clock clock) {
        this.clock = clock;
    }

    /**
     * Return current instant (testable via Clock)
     */
    public Instant nowInstant() {
        return clock.instant();
    }

    /**
     * Get current time in the specified zone.
     *
     * @param zone target timezone
     * @return current ZonedDateTime in the specified zone
     */
    public ZonedDateTime now(ZoneId zone) {
        // explicit: Instant from clock (testable) expressed in the requested zone
        return ZonedDateTime.ofInstant(clock.instant(), zone);
    }

    /**
     * Get current date in the specified zone.
     *
     * @param zone target timezone
     * @return current LocalDate in the specified zone
     */
    public LocalDate today(ZoneId zone) {
        return now(zone).toLocalDate();
    }

    /**
     * Get current time in the context's effective zone.
     * Convenience method - no need to pass ZoneId everywhere.
     *
     * @param ctx request context with effective ZoneId
     * @return current ZonedDateTime in context's zone
     */
    public ZonedDateTime now(TchRequestContext ctx) {
        return now(ctx.tenantZoneId());
    }

    /**
     * Get current date in the context's effective zone.
     * Convenience method - no need to pass ZoneId everywhere.
     *
     * @param ctx request context with effective ZoneId
     * @return current LocalDate in context's zone
     */
    public LocalDate today(TchRequestContext ctx) {
        return today(ctx.tenantZoneId());
    }

    /**
     * Convert a LocalDateTime to Instant using an explicit ZoneId.
     */
    public Instant toInstant(LocalDateTime ldt, ZoneId zone) {
        if (ldt == null) return null;
        ZoneId z = (zone == null) ? ZoneId.of("UTC") : zone;
        return ZonedDateTime.of(ldt, z).toInstant();
    }

    /**
     * Convert a LocalDateTime to Instant using the tenant zone from context if present, otherwise UTC.
     */
    public Instant toInstant(LocalDateTime ldt, TchRequestContext ctx) {
        if (ldt == null) return null;
        ZoneId z = (ctx != null && ctx.tenantZoneId() != null) ? ctx.tenantZoneId() : ZoneId.of("UTC");
        return ZonedDateTime.of(ldt, z).toInstant();
    }
}
