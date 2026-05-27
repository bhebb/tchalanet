package com.tchalanet.server.features.tenantadmin.readiness;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.context.TchContext;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.features.tenantadmin.readiness.model.TenantReadinessView;
import lombok.RequiredArgsConstructor;

/**
 * Handler for {@link GetTenantReadinessQuery}.
 *
 * The effective tenant comes from {@link TchContext} (i.e. RLS / current
 * request context). The handler never trusts a client-supplied tenant id.
 */
@UseCase
@RequiredArgsConstructor
public class GetTenantReadinessQueryHandler
    implements QueryHandler<GetTenantReadinessQuery, TenantReadinessView> {

  private final TenantReadinessAssembler assembler;

  @Override
  public TenantReadinessView handle(GetTenantReadinessQuery query) {
    return assembler.assemble(TchContext.currentOrNull());
  }
}
