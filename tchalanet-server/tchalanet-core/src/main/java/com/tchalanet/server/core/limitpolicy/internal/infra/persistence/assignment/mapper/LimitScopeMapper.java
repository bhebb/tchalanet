package com.tchalanet.server.core.limitpolicy.internal.infra.persistence.assignment.mapper;

import com.tchalanet.server.core.limitpolicy.api.ScopeType;
import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.limitpolicy.api.model.LimitScopeRef;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class LimitScopeMapper {

    public ScopeType toType(LimitScopeRef scope) {
        return switch (scope) {
            case LimitScopeRef.TenantScope ignored -> ScopeType.TENANT;
            case LimitScopeRef.AgentScope ignored -> ScopeType.AGENT;
            case LimitScopeRef.OutletScope ignored -> ScopeType.OUTLET;
            case LimitScopeRef.SellerTerminalScope ignored -> ScopeType.SELLER_TERMINAL;
            case LimitScopeRef.DrawChannelScope ignored -> ScopeType.DRAW_CHANNEL;
        };
    }

    public UUID toId(LimitScopeRef scope) {
        return switch (scope) {
            case LimitScopeRef.TenantScope tenant -> tenant.tenantId().value();
            case LimitScopeRef.AgentScope agent -> agent.userId().value();
            case LimitScopeRef.OutletScope outlet -> outlet.outletId().value();
            case LimitScopeRef.SellerTerminalScope st -> st.sellerTerminalId().value();
            case LimitScopeRef.DrawChannelScope drawChannel -> drawChannel.drawChannelId().value();
        };
    }

    public LimitScopeRef toDomain(ScopeType type, UUID id) {
        if (type == null) {
            throw new IllegalArgumentException("scopeType is required");
        }

        if (id == null) {
            throw new IllegalArgumentException("scopeId is required for " + type);
        }

        return switch (type) {
            case TENANT -> LimitScopeRef.tenant(TenantId.of(id));
            case AGENT -> LimitScopeRef.agent(UserId.of(id));
            case OUTLET -> LimitScopeRef.outlet(OutletId.of(id));
            case SELLER_TERMINAL -> LimitScopeRef.sellerTerminal(SellerTerminalId.of(id));
            case DRAW_CHANNEL -> LimitScopeRef.drawChannel(DrawChannelId.of(id));
            default -> throw new IllegalArgumentException("Unsupported scopeType for LimitPolicy V0: " + type);
        };
    }
}
