package com.tchalanet.server.core.limitpolicy.infra.persistence.adapter;

import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.TenantId;

import com.tchalanet.server.common.types.enums.BetType;
import com.tchalanet.server.common.types.enums.ScopeType;
import com.tchalanet.server.core.limitpolicy.application.port.out.LimitFactsProvider;
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
    public SelectionExposure getSelectionExposure(TenantId tenantId, DrawId drawId, ScopeType scopeType, UUID scopeId, BetType betType, String selectionKey) {
        DrawExposureJpaEntity drawExposureJpaEntity = exposureRepo.findByKey(tenantId.uuid(), drawId.uuid(), scopeType, scopeId, betType, selectionKey);
        if (drawExposureJpaEntity == null) {
            return new SelectionExposure(BigDecimal.ZERO, 0, BigDecimal.ZERO);
        }
        return new SelectionExposure(drawExposureJpaEntity.getStakeTotal(), drawExposureJpaEntity.getSalesCount(), drawExposureJpaEntity.getPotentialPayoutTotal());
    }

    @Override
    public BigDecimal getDrawTotalStake(TenantId tenantId, DrawId drawId, ScopeType scopeType, UUID scopeId) {
        return exposureRepo.sumStakeForDraw(tenantId.uuid(), drawId.uuid(), scopeType, scopeId);
    }

    @Override
    public DailyTotals getDailyTotals(TenantId tenantId, LocalDate day, ScopeType scopeType, UUID scopeId) {
        // For now, stub - need daily stats table
        return new DailyTotals(BigDecimal.ZERO, BigDecimal.ZERO, 0);
    }
}
