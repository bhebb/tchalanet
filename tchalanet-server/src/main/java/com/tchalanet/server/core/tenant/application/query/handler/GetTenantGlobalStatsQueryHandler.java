package com.tchalanet.server.core.tenant.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.types.enums.TenantStatus;
import com.tchalanet.server.core.tenant.application.query.model.GetTenantGlobalStatsQuery;
import com.tchalanet.server.core.tenant.application.query.model.TenantGlobalStatsView;
import com.tchalanet.server.core.tenant.infra.persistence.TenantJpaRepository;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetTenantGlobalStatsQueryHandler
    implements QueryHandler<GetTenantGlobalStatsQuery, TenantGlobalStatsView> {

  private final TenantJpaRepository repo;

  @Override
  public TenantGlobalStatsView handle(GetTenantGlobalStatsQuery query) {
    long total = repo.countByDeletedAtIsNull();
    long active = repo.countByStatusAndDeletedAtIsNull(TenantStatus.ACTIVE);
    long suspended = repo.countByStatusAndDeletedAtIsNull(TenantStatus.SUSPENDED);

    return new TenantGlobalStatsView((int) total, (int) active, (int) suspended);
  }
}
