package com.tchalanet.server.core.session.application.command.model;

import java.util.UUID;

/** Lock session after inactivity - requires PIN to unlock. */
public record LockSessionAfterInactivityCommand(
    UUID tenantId,
    UUID sessionId,
    UUID agentId
) {}

