package com.tchalanet.server.core.session.infra.persistence.mapper;

import com.tchalanet.server.common.types.id.SessionId;
import com.tchalanet.server.common.types.id.TenantId;

import com.tchalanet.server.core.session.domain.model.PosSessionTotals;
import com.tchalanet.server.core.session.infra.persistence.entity.PosSessionTotalsJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class PosSessionTotalsMapper {

    public PosSessionTotals toDomain(PosSessionTotalsJpaEntity entity) {
        return new PosSessionTotals(
            SessionId.nullableOf(entity.getSessionId()),
            TenantId.of(entity.getTenantId()),
            entity.getTotalTickets(),
            entity.getTotalStake(),
            entity.getTotalPayout(),
            entity.getGrossMargin(),
            entity.getUpdatedAt()
        );
    }

    public PosSessionTotalsJpaEntity toEntity(PosSessionTotals domain) {
        var entity = new PosSessionTotalsJpaEntity();
        entity.setSessionId(domain.sessionId().uuid());
        entity.setTenantId(domain.tenantId().uuid());
        entity.setTotalTickets(domain.totalTickets());
        entity.setTotalStake(domain.totalStake());
        entity.setTotalPayout(domain.totalPayout());
        entity.setGrossMargin(domain.grossMargin());
        entity.setUpdatedAt(domain.updatedAt());
        return entity;
    }
}
