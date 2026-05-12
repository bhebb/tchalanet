package com.tchalanet.server.core.draw.internal.infra.web.model;

import com.tchalanet.server.common.types.id.ResultSlotId;

import java.time.LocalTime;

    public record ResultSlotSummaryResponse(
        ResultSlotId id,
        String key,
        String label,
        String timezone,
        LocalTime drawTime
    ) {
    }
