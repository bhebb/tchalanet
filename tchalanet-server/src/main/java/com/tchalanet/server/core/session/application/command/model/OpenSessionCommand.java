package com.tchalanet.server.core.session.application.command.model;

import java.math.BigDecimal;
import java.util.UUID;

public record OpenSessionCommand(
    UUID tenantId, UUID outletId, UUID terminalId, UUID userId, BigDecimal openingFloat) {
}
