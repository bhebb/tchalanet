package com.tchalanet.server.core.limitpolicy.infra.persistence.mapper;

import com.tchalanet.server.common.types.enums.TargetType;
import com.tchalanet.server.common.types.id.AgentId;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.DrawChannelId;

import java.util.UUID;

public final class LimitTargetMapperSupport {

  private LimitTargetMapperSupport() {}

  public static LimitTarget toDomain(TargetType type, UUID targetId) {
    return switch (type) {
      case TENANT -> new LimitTarget.TenantTarget();
      case AGENT -> new LimitTarget.AgentTarget(AgentId.of(targetId));
      case OUTLET -> new LimitTarget.OutletTarget(OutletId.of(targetId));
      case TERMINAL -> new LimitTarget.TerminalTarget(TerminalId.of(targetId));
      case DRAWCHANNEL -> new LimitTarget.DrawChannelTarget(DrawChannelId.of(targetId));
      case GAME, ZONE, RANGE -> throw new IllegalArgumentException("Unsupported target type: " + type);
    };
  }

  /** Returns null for TENANT (DB target_id nullable). */
  public static UUID toUuid(LimitTarget target) {
    if (target instanceof LimitTarget.TenantTarget) return null;
    if (target instanceof LimitTarget.AgentTarget(AgentId id)) return id.value();
    if (target instanceof LimitTarget.OutletTarget(OutletId id)) return id.value();
    if (target instanceof LimitTarget.TerminalTarget(TerminalId id)) return id.value();
    if (target instanceof LimitTarget.DrawChannelTarget(DrawChannelId id)) return id.value();
    throw new IllegalArgumentException("Unsupported LimitTarget variant: " + target);
  }
}
