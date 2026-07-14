package com.tchalanet.server.core.pricing.internal.application.query;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.pricing.api.model.TenantPricingRuleView;
import com.tchalanet.server.core.pricing.api.query.ListTenantPricingRulesQuery;
import com.tchalanet.server.core.pricing.internal.application.mapper.TenantPricingOddsMapper;
import com.tchalanet.server.core.pricing.internal.application.port.out.TenantPricingOddsReaderPort;
import java.util.List;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ListTenantPricingRulesQueryHandler
    implements QueryHandler<ListTenantPricingRulesQuery, List<TenantPricingRuleView>> {

  private final TenantPricingOddsReaderPort reader;
  private final TenantPricingOddsMapper mapper;

  @Override
  public List<TenantPricingRuleView> handle(ListTenantPricingRulesQuery q) {
    return reader.findActiveByTenant(q.tenantId(), q.gameCode()).stream()
        .map(mapper::toView)
        .toList();
  }
}
