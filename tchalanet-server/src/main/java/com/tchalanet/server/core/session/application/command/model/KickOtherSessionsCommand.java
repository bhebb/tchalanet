package com.tchalanet.server.core.session.application.command.model;

import java.util.UUID;

/** Invalidate other sessions for the given agent. */
public record KickOtherSessionsCommand(
    UUID tenantId,
    UUID agentId,
    UUID currentSessionId
) {}

