package com.tchalanet.server.core.subscription.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.subscription.application.port.out.SubscriptionStatsReaderPort;
import com.tchalanet.server.core.subscription.application.query.model.GetPlatformSubscriptionStatsQuery;
import com.tchalanet.server.core.subscription.application.query.model.PlatformSubscriptionStatsView;
import lombok.RequiredArgsConstructor;

/**
 * Handler for platform aggregated subscription stats (cross-tenant read).
 * Read-only, no side effects.
 */
@UseCase
@RequiredArgsConstructor
public class GetPlatformSubscriptionStatsQueryHandler
    implements QueryHandler<GetPlatformSubscriptionStatsQuery, PlatformSubscriptionStatsView> {

  private final SubscriptionStatsReaderPort statsPort;

  @Override
  public PlatformSubscriptionStatsView handle(GetPlatformSubscriptionStatsQuery q) {
    return statsPort.readPlatformStats();
  }
}
