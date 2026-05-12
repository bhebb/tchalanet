package com.tchalanet.server.core.limitpolicy.internal.infra.persistence.exposure;

import com.tchalanet.server.common.types.enums.ScopeType;
import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.limitpolicy.internal.domain.model.LimitScopeRef;

import java.util.UUID;

public final class ScopePersistenceMapper {

    private ScopePersistenceMapper() {
    }

    public static ScopeRow toRow(LimitScopeRef scope) {
        return switch (scope) {
            case LimitScopeRef.TenantScope(TenantId id) -> new ScopeRow(ScopeType.TENANT, id.value());

            case LimitScopeRef.AgentScope(UserId id) -> new ScopeRow(ScopeType.AGENT, id.value());

            case LimitScopeRef.OutletScope(OutletId id) -> new ScopeRow(ScopeType.OUTLET, id.value());

            case LimitScopeRef.DrawChannelScope(DrawChannelId id) -> new ScopeRow(ScopeType.DRAW_CHANNEL, id.value());
        };
    }

    public record ScopeRow(
        ScopeType scopeType,
        UUID scopeId
    ) {
    }
}
