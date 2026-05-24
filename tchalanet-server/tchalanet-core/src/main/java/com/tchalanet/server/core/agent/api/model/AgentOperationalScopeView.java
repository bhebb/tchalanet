package com.tchalanet.server.core.agent.api.model;

import com.tchalanet.server.common.types.id.AgentId;
import com.tchalanet.server.common.types.id.AgentZoneId;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import java.util.List;

public record AgentOperationalScopeView(
    AgentId agentId,
    List<AgentId> agentPath,
    AgentZoneId zoneId,
    List<AgentZoneId> zonePath,
    OutletId outletId,
    TerminalId terminalId,
    UserId sellerUserId,
    boolean eligible,
    List<String> warnings
) {}
