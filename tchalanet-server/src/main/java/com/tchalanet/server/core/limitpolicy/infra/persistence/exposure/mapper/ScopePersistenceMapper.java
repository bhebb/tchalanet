package com.tchalanet.server.core.limitpolicy.infra.persistence.exposure.mapper;

import com.tchalanet.server.common.types.enums.ScopeType;
import com.tchalanet.server.common.types.id.*;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitScopeRef;

import java.util.UUID;

public final class ScopePersistenceMapper {

  private ScopePersistenceMapper() {}

  /** Domain -> persistence (scopeType, scopeId UUID). */
  public static ScopeRow toRow(LimitScopeRef scope, TenantId tenantId) {
    return switch (scope) {
      case LimitScopeRef.TenantScope(TenantId id) -> new ScopeRow(ScopeType.TENANT, id.value());
      case LimitScopeRef.AgentScope(AgentId id) -> new ScopeRow(ScopeType.AGENT, id.value());
      case LimitScopeRef.OutletScope(OutletId id) -> new ScopeRow(ScopeType.OUTLET, id.value());
      case LimitScopeRef.TerminalScope(TerminalId id) -> new ScopeRow(ScopeType.TERMINAL, id.value());
      case LimitScopeRef.DrawChannelScope(DrawChannelId id) -> new ScopeRow(ScopeType.DRAWCHANNEL, id.value());
    };
  }

  public record ScopeRow(ScopeType scopeType, UUID scopeId) {}
}
