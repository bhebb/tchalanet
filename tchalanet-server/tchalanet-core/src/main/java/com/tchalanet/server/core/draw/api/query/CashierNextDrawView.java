package com.tchalanet.server.core.draw.api.query;

import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.ResultSlotId;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Cashier-facing view of an upcoming draw. Exposes IDs and slot metadata so the seller can
 * pivot from this view to a {@code preview} / {@code sell} call. Channel label formatting is the
 * caller's responsibility (the catalog {@code DrawChannelDisplayFormatter} or a feature wrapper).
 */
public record CashierNextDrawView(
    DrawId drawId,
    DrawChannelId drawChannelId,
    ResultSlotId resultSlotId,
    String resultSlotKey,
    String channelCode,
    String channelLabel,
    LocalDate drawDate,
    LocalTime drawTime,
    Instant scheduledAt,
    Instant cutoffAt,
    String status
) {}
