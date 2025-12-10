package com.tchalanet.server.core.session.application.command.model;

import java.util.UUID;

public record LogoutAgentCommand(
    UUID tenantId,
    UUID sessionId,
    UUID agentId
) {}

