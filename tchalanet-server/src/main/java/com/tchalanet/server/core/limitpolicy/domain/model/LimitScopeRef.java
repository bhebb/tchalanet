package com.tchalanet.server.core.limitpolicy.domain.model;

import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;

public sealed interface LimitScopeRef
    permits LimitScopeRef.TenantScope,
    LimitScopeRef.OutletScope,
    LimitScopeRef.AgentScope,
    LimitScopeRef.DrawChannelScope {

    static TenantScope tenant(TenantId tenantId) {
        return new TenantScope(tenantId);
    }

    static OutletScope outlet(OutletId outletId) {
        return new OutletScope(outletId);
    }

    static AgentScope agent(UserId userId) {
        return new AgentScope(userId);
    }

    static DrawChannelScope drawChannel(DrawChannelId drawChannelId) {
        return new DrawChannelScope(drawChannelId);
    }

    record TenantScope(TenantId tenantId) implements LimitScopeRef {
    }

    record OutletScope(OutletId outletId) implements LimitScopeRef {
    }

    record AgentScope(UserId userId) implements LimitScopeRef {
    }

    record DrawChannelScope(DrawChannelId drawChannelId) implements LimitScopeRef {
    }
}
