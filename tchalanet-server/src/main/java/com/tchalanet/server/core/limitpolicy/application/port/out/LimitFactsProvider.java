package com.tchalanet.server.core.limitpolicy.application.port.out;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.TenantId;

import com.tchalanet.server.common.types.enums.BetType;
import com.tchalanet.server.common.types.enums.ScopeType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public interface LimitFactsProvider {
  SelectionExposure getSelectionExposure(TenantId tenantId, DrawId drawId, ScopeType scopeType, UUID scopeId, BetType betType, String selectionKey);

  BigDecimal getDrawTotalStake(TenantId tenantId, DrawId drawId, ScopeType scopeType, UUID scopeId);

  DailyTotals getDailyTotals(TenantId tenantId, LocalDate day, ScopeType scopeType, UUID scopeId);

  record SelectionExposure(BigDecimal stakeTotal, long salesCount, BigDecimal potentialPayoutTotal) {}

  record DailyTotals(BigDecimal stakeTotal, BigDecimal payoutTotal, long cancelCount) {}
}
