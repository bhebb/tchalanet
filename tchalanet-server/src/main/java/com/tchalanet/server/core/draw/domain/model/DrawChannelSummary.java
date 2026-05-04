package com.tchalanet.server.core.draw.domain.model;

import com.tchalanet.server.common.types.id.DrawChannelId;

import java.time.OffsetDateTime;
import java.util.List;


public record DrawChannelSummary(
    DrawChannelId id,
    String name,
    String code
) {}
