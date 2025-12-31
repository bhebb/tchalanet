package com.tchalanet.server.core.game.application.command;

import com.fasterxml.jackson.databind.JsonNode;
import com.tchalanet.server.common.bus.Command;
import java.math.BigDecimal;
import java.util.UUID;

public record UpdateTenantGameCommand(
    UUID gameId,
    Boolean enabled,
    String displayName,
    BigDecimal minStake,
    BigDecimal maxStake,
    JsonNode flags) implements Command<Boolean> {}

