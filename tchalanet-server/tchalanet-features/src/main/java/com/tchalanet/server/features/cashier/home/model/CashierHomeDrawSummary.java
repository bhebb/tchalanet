package com.tchalanet.server.features.cashier.home.model;

import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.DrawId;
import java.time.Instant;

public record CashierHomeDrawSummary(
    DrawId drawId,
    DrawChannelId drawChannelId,
    String label,
    Instant scheduledAt,
    String scheduledAtLabel,
    Instant cutoffAt,
    String cutoffLabel,
    String status) {}
