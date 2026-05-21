package com.tchalanet.server.common.time;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Testable time provider for all "now" computations in business logic.
 *
 * <p>Backed by an injected {@link Clock} so that tests can use a fixed clock.
 * Never use {@code Instant.now()}, {@code LocalDate.now()}, or
 * {@code ZoneId.systemDefault()} directly in domain or application code — use
 * this interface instead.
 *
 * <p>The default implementation is {@link TimeProvider}.
 */
public interface TchTimeProvider {

    /**
     * Returns the current instant from the underlying clock.
     */
    Instant now();

    /**
     * Returns today's date as observed in the given timezone.
     *
     * @param zoneId the semantic owner timezone (channel, tenant, slot…)
     */
    LocalDate today(ZoneId zoneId);

    /**
     * Returns the current date-time as observed in the given timezone.
     *
     * @param zoneId the semantic owner timezone (channel, tenant, slot…)
     */
    ZonedDateTime nowAt(ZoneId zoneId);

    /**
     * Exposes the underlying clock, e.g. for passing to domain services that
     * need it directly.
     */
    Clock clock();
}

