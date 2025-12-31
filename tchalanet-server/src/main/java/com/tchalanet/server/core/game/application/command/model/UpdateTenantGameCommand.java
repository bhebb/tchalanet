package com.tchalanet.server.core.game.application.command.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.GameId;
import java.math.BigDecimal;

public record UpdateTenantGameCommand(
    GameId gameId,
    Boolean enabled,
    String displayName,
    BigDecimal minStake,
    BigDecimal maxStake,
    JsonNode flags)
    implements Command<Boolean> {}
