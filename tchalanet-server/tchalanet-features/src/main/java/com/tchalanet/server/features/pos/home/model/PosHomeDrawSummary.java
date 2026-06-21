package com.tchalanet.server.features.pos.home.model;

import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.DrawId;
import java.time.Instant;

public record PosHomeDrawSummary(
    DrawId drawId,
    DrawChannelId drawChannelId,
    String label,
    Instant scheduledAt,
    String scheduledAtLabel,
    Instant cutoffAt,
    String cutoffLabel,
    String status) {}
