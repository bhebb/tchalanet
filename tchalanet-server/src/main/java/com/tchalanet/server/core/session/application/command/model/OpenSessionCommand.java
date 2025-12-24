package com.tchalanet.server.core.session.application.command.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.core.session.domain.model.PosSession;

import java.math.BigDecimal;
import java.util.UUID;

public record OpenSessionCommand(
    UUID tenantId, UUID outletId, UUID terminalId, UUID userId, BigDecimal openingFloat) implements Command <PosSession>{
}
