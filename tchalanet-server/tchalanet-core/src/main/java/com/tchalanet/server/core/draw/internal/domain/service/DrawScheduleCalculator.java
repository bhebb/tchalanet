package com.tchalanet.server.core.draw.internal.domain.service;

import com.tchalanet.server.core.draw.internal.domain.model.DrawScheduleSnapshot;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Objects;

/**
 * Pure domain calculator that converts a channel schedule definition
 * (date + time-of-day + timezone + cutoff offset) into a {@link DrawScheduleSnapshot}.
 *
 * <p>This class is stateless and has no Spring dependencies; it can be used in
 * both application handlers and unit tests without a Spring context.
 *
 * <p>Usage:
 * <pre>{@code
 * var calculator = new DrawScheduleCalculator();
 * DrawScheduleSnapshot snap = calculator.compute(
 *     drawDate, drawTime, ZoneId.of("America/Port-au-Prince"), Duration.ofMinutes(5));
 * }</pre>
 */
public final class DrawScheduleCalculator {

    /**
     * Computes the {@link DrawScheduleSnapshot} for one draw occurrence.
     *
     * @param drawDate         channel-local commercial date of the draw
     * @param drawTime         time-of-day when the draw is scheduled (channel timezone)
     * @param zoneId           timezone that owns the draw schedule
     * @param cutoffBeforeDraw how long before {@code scheduledAt} the cutoff is set
     * @return a fully populated {@link DrawScheduleSnapshot}
     */
    public DrawScheduleSnapshot compute(
        LocalDate drawDate,
        LocalTime drawTime,
        ZoneId zoneId,
        Duration cutoffBeforeDraw
    ) {
        Objects.requireNonNull(drawDate, "drawDate is required");
        Objects.requireNonNull(drawTime, "drawTime is required");
        Objects.requireNonNull(zoneId, "zoneId is required");
        Objects.requireNonNull(cutoffBeforeDraw, "cutoffBeforeDraw is required");

        if (cutoffBeforeDraw.isNegative()) {
            throw new IllegalArgumentException("cutoffBeforeDraw must not be negative");
        }
        if (cutoffBeforeDraw.isZero()) {
            throw new IllegalArgumentException("cutoffBeforeDraw must be > 0 (cutoff must be strictly before draw)");
        }

        var scheduledAt = ZonedDateTime.of(drawDate, drawTime, zoneId).toInstant();
        var cutoffAt = scheduledAt.minus(cutoffBeforeDraw);

        return new DrawScheduleSnapshot(drawDate, drawTime, zoneId, scheduledAt, cutoffAt);
    }
}

