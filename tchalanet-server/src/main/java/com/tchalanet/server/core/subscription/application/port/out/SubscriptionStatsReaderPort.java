package com.tchalanet.server.core.subscription.application.port.out;

import com.tchalanet.server.core.subscription.application.query.model.PlatformSubscriptionStatsView;

public interface SubscriptionStatsReaderPort {
  PlatformSubscriptionStatsView readPlatformStats();
}
