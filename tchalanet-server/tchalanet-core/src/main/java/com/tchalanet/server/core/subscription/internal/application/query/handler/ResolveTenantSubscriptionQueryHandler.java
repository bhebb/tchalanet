package com.tchalanet.server.core.subscription.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.subscription.internal.application.port.out.SubscriptionReaderPort;
import com.tchalanet.server.core.subscription.api.query.ResolveTenantSubscriptionQuery;
import com.tchalanet.server.core.subscription.api.query.SubscriptionView;
import lombok.RequiredArgsConstructor;

/**
 * Handler for ResolveTenantSubscriptionQuery.
 * Maps to spec requirement S2 (resolve tenant subscription).
 * Read-only, no transaction needed.
 */
@UseCase
@RequiredArgsConstructor
public class ResolveTenantSubscriptionQueryHandler
    implements QueryHandler<ResolveTenantSubscriptionQuery, SubscriptionView> {

  private final SubscriptionReaderPort readerPort;

  @Override
  public SubscriptionView handle(ResolveTenantSubscriptionQuery query) {
    return readerPort.findByTenantId(query.tenantId())
        .map(s -> new SubscriptionView(
            s.tenantId(),
            s.planCode(),
            s.status(),
            s.startedAt(),
            s.endsAt(),
            s.version(),
            s.updatedAt()
        ))
        .orElse(null);
  }
}
