package com.tchalanet.server.core.sales.api.model.print;

import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.DrawId;

import java.time.Instant;
import java.time.LocalDate;

public record TicketPrintDraw(
    DrawId drawId,
    DrawChannelId drawChannelId,
    String label,
    String drawChannelName,
    LocalDate drawDate,
    Instant scheduledAt,
    Instant cutoffAt
) {
}
