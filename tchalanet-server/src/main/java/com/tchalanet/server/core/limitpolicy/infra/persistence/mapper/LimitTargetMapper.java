package com.tchalanet.server.core.limitpolicy.infra.persistence.mapper;

import com.tchalanet.server.common.types.enums.TargetType;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitTarget;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class LimitTargetMapper {

    public TargetType toType(LimitTarget t) {
        return t.type();
    }

    public UUID toIdOrNull(LimitTarget t) {
        return switch (t) {
            case LimitTarget.TenantTarget ignored -> null;
            case LimitTarget.AgentTarget a -> a.id().value();
            case LimitTarget.OutletTarget o -> o.id().value();
            case LimitTarget.TerminalTarget te -> te.id().value();
            case LimitTarget.DrawChannelTarget dc -> dc.id().value();
        };
    }

    public LimitTarget toDomain(TargetType type, UUID id) {
        return switch (type) {
            case TENANT -> new LimitTarget.TenantTarget();
            case AGENT -> new LimitTarget.AgentTarget(com.tchalanet.server.common.types.id.AgentId.of(id));
            case OUTLET -> new LimitTarget.OutletTarget(com.tchalanet.server.common.types.id.OutletId.of(id));
            case TERMINAL -> new LimitTarget.TerminalTarget(com.tchalanet.server.common.types.id.TerminalId.of(id));
            case DRAWCHANNEL ->
                new LimitTarget.DrawChannelTarget(com.tchalanet.server.common.types.id.DrawChannelId.of(id));
            default -> throw new IllegalArgumentException("Unsupported targetType for LimitTarget: " + type);
        };
    }
}
