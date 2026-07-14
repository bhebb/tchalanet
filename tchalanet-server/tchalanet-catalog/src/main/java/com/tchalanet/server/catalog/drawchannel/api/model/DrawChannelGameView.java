package com.tchalanet.server.catalog.drawchannel.api.model;

import com.tchalanet.server.common.types.id.DrawChannelGameId;
import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.TenantGameId;
import tools.jackson.databind.JsonNode;

public record DrawChannelGameView(
    DrawChannelGameId id,
    DrawChannelId drawChannelId,
    TenantGameId tenantGameId,
    boolean enabled,
    JsonNode flags) {}
