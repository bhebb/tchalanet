package com.tchalanet.server.core.session.application.command.model;

import java.util.UUID;

/** Login command for agent (PIN auth). */
public record LoginAgentCommand(
    UUID tenantId,
    UUID agentId,
    String pin
) {}

