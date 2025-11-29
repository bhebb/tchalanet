package com.tchalanet.server.draw.application.command.model;

import java.util.UUID;

public record RetrySettleDrawCommand(UUID tenantId, UUID drawId, UUID triggeredBy) {}
