package com.tchalanet.server.core.draw.internal.infra.web.model;

import com.tchalanet.server.common.types.id.ResultSlotId;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Query params for {@code GET /admin/draws}.
 *
 * <p>{@code from}/{@code to} filter on the business date; {@code scheduledBefore}/{@code
 * scheduledAfter} filter on the draw instant, which is what "past" and "upcoming" actually mean
 * within the current day.
 */
public record DrawSearchRequest(
    ResultSlotId resultSlotId,
    String status,
    LocalDate from,
    LocalDate to,
    Instant scheduledBefore,
    Instant scheduledAfter) {}
