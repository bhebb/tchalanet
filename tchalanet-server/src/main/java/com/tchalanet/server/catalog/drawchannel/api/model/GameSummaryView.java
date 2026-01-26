package com.tchalanet.server.catalog.drawchannel.api.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.tchalanet.server.common.types.id.GameId;

public record GameSummaryView(
    GameId gameId,
    String gameCode,
    boolean enabled,
    JsonNode flags) {}
