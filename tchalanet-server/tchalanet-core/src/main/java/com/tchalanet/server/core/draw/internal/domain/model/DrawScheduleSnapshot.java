package com.tchalanet.server.core.draw.internal.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

/**
 * Immutable schedule computed for a draw occurrence.
 *
 * <p>{@code drawDate} is the channel-local commercial date of the draw.
 * {@code scheduledAt} and {@code cutoffAt} are UTC instants stored in the DB
 * as {@code timestamptz}.
 */
public record DrawScheduleSnapshot(
    LocalDate drawDate,
    LocalTime drawTime,
    ZoneId zoneId,
    Instant scheduledAt,
    Instant cutoffAt
) {}

