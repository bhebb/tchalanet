package com.tchalanet.server.core.limitpolicy.domain.model;

import com.tchalanet.server.common.types.enums.ScopeType;
import com.tchalanet.server.common.types.id.*;

public sealed interface LimitScopeRef permits
    LimitScopeRef.TenantScope,
    LimitScopeRef.OutletScope,
    LimitScopeRef.TerminalScope,
    LimitScopeRef.AgentScope,
    LimitScopeRef.DrawChannelScope {

  ScopeType scopeType();

  /** Stable, audit-safe, UI-safe. Ex: "OUTLET:<uuid>" */
  String key();

  /** TenantScope - note: uses TenantId, no UUID leaks. */
  record TenantScope(TenantId tenantId) implements LimitScopeRef {
    @Override public ScopeType scopeType() { return ScopeType.TENANT; }
    @Override public String key() { return "TENANT:" + tenantId.value(); }
  }

  record OutletScope(OutletId outletId) implements LimitScopeRef {
    @Override public ScopeType scopeType() { return ScopeType.OUTLET; }
    @Override public String key() { return "OUTLET:" + outletId.value(); }
  }

  record TerminalScope(TerminalId terminalId) implements LimitScopeRef {
    @Override public ScopeType scopeType() { return ScopeType.TERMINAL; }
    @Override public String key() { return "TERMINAL:" + terminalId.value(); }
  }

  record AgentScope(AgentId agentId) implements LimitScopeRef {
    @Override public ScopeType scopeType() { return ScopeType.AGENT; }
    @Override public String key() { return "AGENT:" + agentId.value(); }
  }

  record DrawChannelScope(DrawChannelId drawChannelId) implements LimitScopeRef {
    @Override public ScopeType scopeType() { return ScopeType.DRAWCHANNEL; }
    @Override public String key() { return "DRAWCHANNEL:" + drawChannelId.value(); }
  }
}
