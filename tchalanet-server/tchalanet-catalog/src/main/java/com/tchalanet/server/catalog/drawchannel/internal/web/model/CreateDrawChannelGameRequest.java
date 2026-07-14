package com.tchalanet.server.catalog.drawchannel.internal.web.model;

import com.tchalanet.server.common.types.id.TenantGameId;
import jakarta.validation.constraints.NotNull;
import tools.jackson.databind.JsonNode;

public record CreateDrawChannelGameRequest(
    @NotNull TenantGameId tenantGameId, boolean enabled, JsonNode flags) {}
