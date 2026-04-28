package com.tchalanet.server.core.session.infra.persistence.mapper;

import com.tchalanet.server.common.types.id.SessionId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.session.domain.model.SalesSessionTotals;
import com.tchalanet.server.core.session.infra.persistence.entity.SalesSessionTotalsJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class SalesSessionTotalsMapper {

  public SalesSessionTotals toDomain(SalesSessionTotalsJpaEntity entity) {
    return new SalesSessionTotals(
        SessionId.nullableOf(entity.getSessionId()),
        TenantId.of(entity.getTenantId()),
        entity.getTotalTickets(),
        entity.getTotalStake(),
        entity.getTotalPayout(),
        entity.getGrossMargin(),
        entity.getUpdatedAt());
  }

  public SalesSessionTotalsJpaEntity toEntity(SalesSessionTotals domain) {
    var entity = new SalesSessionTotalsJpaEntity();
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
