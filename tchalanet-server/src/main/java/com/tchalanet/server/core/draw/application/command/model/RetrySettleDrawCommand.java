package com.tchalanet.server.core.draw.application.command.model;

import java.util.UUID;

public record RetrySettleDrawCommand(UUID tenantId, UUID drawId, UUID triggeredBy) {}
