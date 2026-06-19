package com.tchalanet.server.core.limitpolicy.internal.infra.persistence.exposure;

import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.limitpolicy.api.ScopeType;
import com.tchalanet.server.core.limitpolicy.api.model.LimitScopeRef;

import java.util.UUID;

public final class ScopePersistenceMapper {

    private ScopePersistenceMapper() {
    }

    public static ScopeRow toRow(LimitScopeRef scope) {
        return switch (scope) {
            case LimitScopeRef.TenantScope(TenantId id) -> new ScopeRow(ScopeType.TENANT, id.value());

            case LimitScopeRef.SellerTerminalScope(SellerTerminalId id) ->
                new ScopeRow(ScopeType.SELLER_TERMINAL, id.value());
            case LimitScopeRef.AgentScope(UserId id) -> new ScopeRow(ScopeType.AGENT, id.value());

            case LimitScopeRef.DrawChannelScope(DrawChannelId id) -> new ScopeRow(ScopeType.DRAW_CHANNEL, id.value());
        };
    }

    public record ScopeRow(
        ScopeType scopeType,
        UUID scopeId
    ) {
    }
}
