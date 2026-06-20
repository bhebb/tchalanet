package com.tchalanet.server.core.limitpolicy.api.model;

import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;

public sealed interface LimitScopeRef
    permits LimitScopeRef.TenantScope,
    LimitScopeRef.AgentScope,
    LimitScopeRef.SellerTerminalScope,
    LimitScopeRef.DrawChannelScope {

    static TenantScope tenant(TenantId tenantId) {
        return new TenantScope(tenantId);
    }

    static AgentScope agent(UserId userId) {
        return new AgentScope(userId);
    }

    static SellerTerminalScope sellerTerminal(SellerTerminalId sellerTerminalId) {
        return new SellerTerminalScope(sellerTerminalId);
    }

    static DrawChannelScope drawChannel(DrawChannelId drawChannelId) {
        return new DrawChannelScope(drawChannelId);
    }

    record TenantScope(TenantId tenantId) implements LimitScopeRef {
    }

    record AgentScope(UserId userId) implements LimitScopeRef {
    }

    record SellerTerminalScope(SellerTerminalId sellerTerminalId) implements LimitScopeRef {
    }

    record DrawChannelScope(DrawChannelId drawChannelId) implements LimitScopeRef {
    }
}
