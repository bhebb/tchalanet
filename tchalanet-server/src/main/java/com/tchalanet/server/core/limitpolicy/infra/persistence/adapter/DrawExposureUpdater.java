package com.tchalanet.server.core.limitpolicy.infra.persistence.adapter;

import com.tchalanet.server.common.types.enums.ScopeType;
import com.tchalanet.server.common.types.id.AgentId;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitContext;
import com.tchalanet.server.core.limitpolicy.infra.persistence.repository.DrawExposureJpaRepository;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class DrawExposureUpdater {

  private final DrawExposureJpaRepository repo;

  @Transactional
  public void updateExposureForSale(LimitContext context) {
    // Update for various scopes: AGENT, OUTLET, ZONE, RANGE, TENANT
    updateForScope(context, ScopeType.AGENT, context.agentId());
    updateForScope(context, ScopeType.OUTLET, context.outletId());
    if (context.zoneId() != null) {
      updateForScope(context, ScopeType.ZONE, context.zoneId());
    }
    if (context.rangeIds() != null) {
      for (String rangeId : context.rangeIds()) {
        updateForScope(context, ScopeType.RANGE, rangeId);
      }
    }
    updateForScope(context, ScopeType.TENANT, context.tenantId()); // scopeId = tenantId for TENANT
  }

  private void updateForScope(LimitContext context, ScopeType scopeType, Object scopeId) {
    UUID tenantUuid = toUuid(context.tenantId());
    UUID drawUuid = toUuid(context.drawId());
    UUID scopeUuid = toUuid(scopeId);

    // For each line, update selection exposure
    for (var line : context.lines()) {
      repo.incrementExposure(
          tenantUuid,
          drawUuid,
          scopeType,
          scopeUuid,
          line.betType(),
          line.selectionKey(),
          line.stake(),
          1L, // sales count
          line.stake().multiply(line.optionalMultiplier()) // potential payout
          );
    }

    // Update total for draw
    repo.incrementExposure(
        tenantUuid,
        drawUuid,
        scopeType,
        scopeUuid,
        null, // betType
        null, // selectionKey
        context.ticketStakeTotal(),
        0L, // no count for total
        BigDecimal.ZERO // no payout for total
        );
  }

  private UUID toUuid(Object id) {
    if (id == null) return null;
    if (id instanceof UUID u) return u;
    if (id instanceof TenantId) return ((TenantId) id).uuid();
    if (id instanceof DrawId) return ((DrawId) id).uuid();
    if (id instanceof AgentId) return ((AgentId) id).uuid();
    if (id instanceof OutletId) return ((OutletId) id).uuid();
    if (id instanceof TerminalId) return ((TerminalId) id).uuid();
    if (id instanceof String s) {
      try {
        return UUID.fromString(s);
      } catch (Exception ex) {
        return null;
      }
    }
    return null;
  }
}
