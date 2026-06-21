package com.tchalanet.server.features.pos.draws;

import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.ResultSlotId;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record PosAvailableDrawView(
    DrawId drawId,
    DrawChannelId drawChannelId,
    LocalDate drawDate,
    ResultSlotId resultSlotId,
    String resultSlotKey,
    String channelCode,
    String channelLabel,
    List<String> gameCodes,
    String status,
    Instant scheduledAt,
    Instant cutoffAt
) {}
