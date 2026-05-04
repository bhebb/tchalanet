package com.tchalanet.server.core.draw.domain.model;

import com.tchalanet.server.common.types.id.ResultSlotId;

import java.time.LocalTime;

    public record ResultSlotSummary(
        ResultSlotId id,
        String key,
        String label,
        String timezone,
        LocalTime drawTime
    ) {
    }
