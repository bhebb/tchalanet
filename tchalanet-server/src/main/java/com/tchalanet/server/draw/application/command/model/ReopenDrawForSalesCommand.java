package com.tchalanet.server.draw.application.command.model;

import java.util.UUID;

public record ReopenDrawForSalesCommand(UUID tenantId, UUID drawId, UUID reopenedBy) {}
