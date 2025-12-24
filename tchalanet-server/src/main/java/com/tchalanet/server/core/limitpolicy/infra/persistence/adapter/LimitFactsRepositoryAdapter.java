package com.tchalanet.server.core.limitpolicy.infra.persistence.adapter;

import com.tchalanet.server.core.limitpolicy.application.port.out.LimitFactsProvider;
import com.tchalanet.server.core.limitpolicy.domain.model.ScopeType;
import com.tchalanet.server.core.limitpolicy.infra.persistence.entity.DrawExposureJpaEntity;
import com.tchalanet.server.core.limitpolicy.infra.persistence.repository.DrawExposureJpaRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LimitFactsRepositoryAdapter implements LimitFactsProvider {

  private final DrawExposureJpaRepository exposureRepo;


    @Override
    public SelectionExposure getSelectionExposure(UUID tenantId, UUID drawId, ScopeType scopeType, UUID scopeId, BetType betType, String selectionKey) {
        DrawExposureJpaEntity e = exposureRepo.findByKey(tenantId, drawId, scopeType, scopeId, betType, selectionKey);
        if (e == null) {
            return new SelectionExposure(BigDecimal.ZERO, 0, BigDecimal.ZERO);
        }
        return new SelectionExposure(e.getStakeTotal(), e.getSalesCount(), e.getPotentialPayoutTotal());
    }

    @Override
  public BigDecimal getDrawTotalStake(UUID tenantId, UUID drawId, ScopeType scopeType, UUID scopeId) {
    return exposureRepo.sumStakeForDraw(tenantId, drawId, scopeType, scopeId);
  }

  @Override
  public DailyTotals getDailyTotals(UUID tenantId, LocalDate day, ScopeType scopeType, UUID scopeId) {
    // For now, stub - need daily stats table
    return new DailyTotals(BigDecimal.ZERO, BigDecimal.ZERO, 0);
  }
}
