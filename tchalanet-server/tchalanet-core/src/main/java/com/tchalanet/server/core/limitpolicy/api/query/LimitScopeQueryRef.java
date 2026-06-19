package com.tchalanet.server.core.limitpolicy.api.query;

import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import java.util.UUID;

public record LimitScopeQueryRef(Type type, UUID id) {

  public enum Type {
    TENANT,
    AGENT,
    SELLER_TERMINAL,
    DRAW_CHANNEL
  }

  public static LimitScopeQueryRef tenant(TenantId tenantId) {
    return new LimitScopeQueryRef(Type.TENANT, tenantId.value());
  }

  public static LimitScopeQueryRef agent(UserId userId) {
    return new LimitScopeQueryRef(Type.AGENT, userId.value());
  }

  public static LimitScopeQueryRef sellerTerminal(SellerTerminalId sellerTerminalId) {
    return new LimitScopeQueryRef(Type.SELLER_TERMINAL, sellerTerminalId.value());
  }

  public static LimitScopeQueryRef drawChannel(DrawChannelId drawChannelId) {
    return new LimitScopeQueryRef(Type.DRAW_CHANNEL, drawChannelId.value());
  }
}
