package com.tchalanet.server.catalog.drawchannel.api.model;

import tools.jackson.databind.JsonNode;
import com.tchalanet.server.common.types.id.TenantGameId;

public record GameSummaryView(
    TenantGameId tenantGameId,
    String gameCode,
    boolean enabled,
    JsonNode flags) {}
