package com.tchalanet.server.catalog.drawchannel.internal.web.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.tchalanet.server.common.types.id.DrawChannelGameId;
import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.GameId;

public record DrawChannelGameResponse(
    DrawChannelGameId id,
    DrawChannelId drawChannelId,
    GameId gameId,
    boolean enabled,
    JsonNode flags) {}
