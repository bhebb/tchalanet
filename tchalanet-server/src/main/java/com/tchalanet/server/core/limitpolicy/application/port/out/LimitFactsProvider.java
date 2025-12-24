package com.tchalanet.server.core.limitpolicy.application.port.out;

import com.tchalanet.server.core.limitpolicy.domain.model.ScopeType;
import com.tchalanet.server.core.sales.domain.model.BetType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public interface LimitFactsProvider {
  SelectionExposure getSelectionExposure(UUID tenantId, UUID drawId, ScopeType scopeType, UUID scopeId, BetType betType, String selectionKey);

  BigDecimal getDrawTotalStake(UUID tenantId, UUID drawId, ScopeType scopeType, UUID scopeId);

  DailyTotals getDailyTotals(UUID tenantId, LocalDate day, ScopeType scopeType, UUID scopeId);

  record SelectionExposure(BigDecimal stakeTotal, long salesCount, BigDecimal potentialPayoutTotal) {}

  record DailyTotals(BigDecimal stakeTotal, BigDecimal payoutTotal, long cancelCount) {}
}
