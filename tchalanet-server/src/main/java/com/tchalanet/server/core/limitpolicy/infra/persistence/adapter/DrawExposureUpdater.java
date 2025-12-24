package com.tchalanet.server.core.limitpolicy.infra.persistence.adapter;

import com.tchalanet.server.core.limitpolicy.domain.model.LimitContext;
import com.tchalanet.server.core.limitpolicy.domain.model.ScopeType;
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
      for (UUID rangeId : context.rangeIds()) {
        updateForScope(context, ScopeType.RANGE, rangeId);
      }
    }
    updateForScope(context, ScopeType.TENANT, context.tenantId()); // scopeId = tenantId for TENANT
  }

  private void updateForScope(LimitContext context, ScopeType scopeType, UUID scopeId) {
    // For each line, update selection exposure
    for (var line : context.lines()) {
      repo.incrementExposure(
          context.tenantId(),
          context.drawId(),
          scopeType.name(),
          scopeId,
          line.betType(),
          line.selectionKey(),
          line.stake(),
          1, // sales count
          line.stake().multiply(line.optionalMultiplier()) // potential payout
      );
    }

    // Update total for draw
    repo.incrementExposure(
        context.tenantId(),
        context.drawId(),
        scopeType.name(),
        scopeId,
        null, // betType
        null, // selectionKey
        context.ticketStakeTotal(),
        0, // no count for total
        BigDecimal.ZERO // no payout for total
    );
  }
}
