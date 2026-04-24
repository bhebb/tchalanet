package com.tchalanet.server.core.autonomy.application.service.model;

import com.tchalanet.server.common.types.id.AgentId;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TerminalId;
import java.time.Instant;

public record AutonomyResolveRequest(
    AgentId agentId,
    TerminalId terminalId,
    OutletId outletId,
    Instant now
) {}
