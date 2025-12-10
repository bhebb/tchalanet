package com.tchalanet.server.core.session.application.command.model;

import java.util.UUID;

/** Agent changes outlet/location */
public record SwitchLocationCommand(
    UUID tenantId,
    UUID agentId,
    UUID fromOutletId,
    UUID toOutletId
) {}

