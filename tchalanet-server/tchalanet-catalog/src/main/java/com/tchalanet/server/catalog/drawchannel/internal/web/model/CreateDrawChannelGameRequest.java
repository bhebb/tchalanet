package com.tchalanet.server.catalog.drawchannel.internal.web.model;

import tools.jackson.databind.JsonNode;
import com.tchalanet.server.common.types.id.TenantGameId;
import jakarta.validation.constraints.NotNull;

public record CreateDrawChannelGameRequest(
    @NotNull TenantGameId tenantGameId,
    boolean enabled,
    JsonNode flags) {}
