package com.tchalanet.server.core.limitpolicy.infra.persistence.exposure.adapter;

import com.tchalanet.server.common.context.TchContextResolver;
import com.tchalanet.server.core.limitpolicy.application.port.out.ExposureAlertsReaderPort;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitScopeRef;
import com.tchalanet.server.core.limitpolicy.infra.persistence.exposure.mapper.ScopePersistenceMapper;
import com.tchalanet.server.core.limitpolicy.infra.persistence.exposure.repo.DrawExposureJpaRepository;
import com.tchalanet.server.common.types.id.DrawId;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ExposureAlertsReaderAdapter implements ExposureAlertsReaderPort {

  private final DrawExposureJpaRepository repo;
  private final TchContextResolver ctxResolver;

  @Override
  public List<Row> topByStake(DrawId drawId, LimitScopeRef scope, int limit) {
    var tenantId = ctxResolver.currentOrThrow().tenantId();
    var s = ScopePersistenceMapper.toRow(scope, tenantId);

    var rows = repo.topByStake(drawId.value(), s.scopeType(), s.scopeId(), PageRequest.of(0, Math.max(1, limit)));
    return rows.stream().map(e -> new Row(
        e.getBetType(), e.getSelectionKey(),
        e.getStakeTotal(), e.getPotentialPayoutTotal(), e.getSalesCount()
    )).toList();
  }

  @Override
  public List<Row> topByPayout(DrawId drawId, LimitScopeRef scope, int limit) {
    var tenantId = ctxResolver.currentOrThrow().tenantId();
    var s = ScopePersistenceMapper.toRow(scope, tenantId);

    var rows = repo.topByPayout(drawId.value(), s.scopeType(), s.scopeId(), PageRequest.of(0, Math.max(1, limit)));
    return rows.stream().map(e -> new Row(
        e.getBetType(), e.getSelectionKey(),
        e.getStakeTotal(), e.getPotentialPayoutTotal(), e.getSalesCount()
    )).toList();
  }
}
