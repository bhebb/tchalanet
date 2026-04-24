package com.tchalanet.server.core.limitpolicy.infra.persistence.exposure.mapper;

import com.tchalanet.server.common.types.enums.ScopeType;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitScopeRef;

import java.util.UUID;

public final class ScopePersistenceMapper {

  private ScopePersistenceMapper() {}

  /** Domain -> persistence (scopeType, scopeId UUID). */
  public static ScopeRow toRow(LimitScopeRef scope, TenantId tenantId) {
    return switch (scope) {
      case LimitScopeRef.TenantScope -> new ScopeRow(ScopeType.TENANT, tenantId.value());
      case LimitScopeRef.AgentScope s -> new ScopeRow(ScopeType.AGENT, s.agentId().value());
      case LimitScopeRef.OutletScope s -> new ScopeRow(ScopeType.OUTLET, s.outletId().value());
      case LimitScopeRef.TerminalScope s -> new ScopeRow(ScopeType.TERMINAL, s.terminalId().value());
      case LimitScopeRef.DrawChannelScope s -> new ScopeRow(ScopeType.DRAWCHANNEL, s.drawChannelId().value());
    };
  }

  public record ScopeRow(ScopeType scopeType, UUID scopeId) {}
}
