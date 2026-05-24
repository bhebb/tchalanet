package com.tchalanet.server.core.agent.api.query;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.agent.api.model.AgentOperationalScopeView;
import jakarta.validation.constraints.NotNull;

public record ResolveAgentOperationalScopeQuery(
    @NotNull TenantId tenantId,
    OutletId outletId,
    TerminalId terminalId,
    SalesSessionId salesSessionId,
    @NotNull UserId sellerUserId
) implements Query<AgentOperationalScopeView> {}
