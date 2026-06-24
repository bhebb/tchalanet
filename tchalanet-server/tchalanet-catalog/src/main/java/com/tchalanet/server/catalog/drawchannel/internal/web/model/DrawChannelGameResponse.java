package com.tchalanet.server.catalog.drawchannel.internal.web.model;

import tools.jackson.databind.JsonNode;
import com.tchalanet.server.common.types.id.DrawChannelGameId;
import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.TenantGameId;

public record DrawChannelGameResponse(
    DrawChannelGameId id,
    DrawChannelId drawChannelId,
    TenantGameId tenantGameId,
    boolean enabled,
    JsonNode flags) {}
