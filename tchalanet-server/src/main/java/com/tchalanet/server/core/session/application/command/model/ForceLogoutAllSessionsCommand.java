package com.tchalanet.server.core.session.application.command.model;

import java.util.UUID;

/** Supervisor/admin forces logout for an agent across all sessions */
public record ForceLogoutAllSessionsCommand(
    UUID tenantId,
    UUID targetAgentId,
    UUID performedBy
) {}

