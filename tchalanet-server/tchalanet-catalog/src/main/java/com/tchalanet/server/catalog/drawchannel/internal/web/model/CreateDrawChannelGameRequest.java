package com.tchalanet.server.catalog.drawchannel.internal.web.model;

import tools.jackson.databind.JsonNode;
import com.tchalanet.server.common.types.id.GameId;
import jakarta.validation.constraints.NotNull;

public record CreateDrawChannelGameRequest(
    @NotNull GameId gameId,
    boolean enabled,
    JsonNode flags) {}
