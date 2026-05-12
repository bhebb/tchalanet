package com.tchalanet.server.core.draw.internal.infra.web.model;

import com.tchalanet.server.common.types.id.DrawChannelId;


public record DrawChannelSummaryResponse(
    String id,
    String name,
    String code
) {}
