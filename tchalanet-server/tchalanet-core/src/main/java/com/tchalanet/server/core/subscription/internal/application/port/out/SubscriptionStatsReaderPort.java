package com.tchalanet.server.core.subscription.internal.application.port.out;

import com.tchalanet.server.core.subscription.application.query.model.PlatformSubscriptionStatsView;

public interface SubscriptionStatsReaderPort {
  PlatformSubscriptionStatsView readPlatformStats();
}
