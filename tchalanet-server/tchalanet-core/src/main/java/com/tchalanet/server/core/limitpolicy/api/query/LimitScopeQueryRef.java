package com.tchalanet.server.core.limitpolicy.api.query;

import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import java.util.UUID;

public record LimitScopeQueryRef(Type type, UUID id) {

  public enum Type {
    TENANT,
    OUTLET,
    AGENT,
    DRAW_CHANNEL
  }

  public static LimitScopeQueryRef tenant(TenantId tenantId) {
    return new LimitScopeQueryRef(Type.TENANT, tenantId.value());
  }

  public static LimitScopeQueryRef outlet(OutletId outletId) {
    return new LimitScopeQueryRef(Type.OUTLET, outletId.value());
  }

  public static LimitScopeQueryRef agent(UserId userId) {
    return new LimitScopeQueryRef(Type.AGENT, userId.value());
  }

  public static LimitScopeQueryRef drawChannel(DrawChannelId drawChannelId) {
    return new LimitScopeQueryRef(Type.DRAW_CHANNEL, drawChannelId.value());
  }
}
