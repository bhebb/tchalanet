package com.tchalanet.server.core.limitpolicy.infra.persistence.exposure.adapter;

import com.tchalanet.server.common.context.TchContextResolver;
import com.tchalanet.server.core.limitpolicy.application.port.out.ExposureFactsReaderPort;
import com.tchalanet.server.core.limitpolicy.domain.model.*;
import com.tchalanet.server.core.limitpolicy.infra.persistence.exposure.mapper.ScopePersistenceMapper;
import com.tchalanet.server.core.limitpolicy.infra.persistence.exposure.repo.DrawExposureJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

@Component
@RequiredArgsConstructor
public class ExposureFactsReaderAdapter implements ExposureFactsReaderPort {

  private final DrawExposureJpaRepository repo;
  private final TchContextResolver ctxResolver;

  @Override
  public LimitFactsSnapshot snapshot(LimitContext ctx) {
    var reqCtx = ctxResolver.currentOrThrow();
    var tenantId = reqCtx.tenantId(); // typed TenantId in your context (assumed)

    var scopeRow = ScopePersistenceMapper.toRow(ctx.scope(), tenantId);

    var betTypes = ctx.lines().stream().map(LimitContext.BetLine::betType).distinct().toList();
    if (betTypes.isEmpty()) {
      return new LimitFactsSnapshot(Map.of());
    }

    var rows = repo.findFactsForBetTypes(
        ctx.drawId().value(),
        scopeRow.scopeType(),
        scopeRow.scopeId(),
        betTypes
    );

    Map<LimitFactsSnapshot.Key, LimitFactsSnapshot.Fact> map = new HashMap<>();
    for (var e : rows) {
      var key = new LimitFactsSnapshot.Key(e.getBetType(), e.getSelectionKey());
      map.put(key, new LimitFactsSnapshot.Fact(
          nz(e.getStakeTotal()),
          nz(e.getPotentialPayoutTotal()),
          e.getSalesCount()
      ));
    }
    return new LimitFactsSnapshot(Collections.unmodifiableMap(map));
  }

  private static BigDecimal nz(BigDecimal v) {
    return v == null ? BigDecimal.ZERO : v;
  }
}
