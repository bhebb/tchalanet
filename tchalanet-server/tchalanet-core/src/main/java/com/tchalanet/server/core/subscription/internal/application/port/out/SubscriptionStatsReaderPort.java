package com.tchalanet.server.core.subscription.internal.application.port.out;

import com.tchalanet.server.core.subscription.api.query.PlatformSubscriptionStatsView;

public interface SubscriptionStatsReaderPort {
  PlatformSubscriptionStatsView readPlatformStats();
}
