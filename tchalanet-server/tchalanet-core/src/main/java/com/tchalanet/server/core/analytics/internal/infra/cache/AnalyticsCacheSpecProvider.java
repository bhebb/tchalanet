package com.tchalanet.server.core.analytics.internal.infra.cache;

import com.tchalanet.server.common.cache.CacheSpec;
import com.tchalanet.server.common.cache.CacheSpecProvider;
import java.time.Duration;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class AnalyticsCacheSpecProvider implements CacheSpecProvider {

  @Override
  public List<CacheSpec> cacheSpecs() {
    return List.of(
        // Cashier today: refreshed frequently (session KPIs are live)
        CacheSpec.of(AnalyticsCacheNames.CASHIER_TODAY,
            Duration.ofSeconds(30), Duration.ofMinutes(2)),
        // Admin overview: slightly longer — aggregates across tenants/outlets
        CacheSpec.of(AnalyticsCacheNames.ADMIN_OVERVIEW,
            Duration.ofMinutes(1), Duration.ofMinutes(5)),
        // Sales summary report: longer TTL — used for historical date ranges
        CacheSpec.of(AnalyticsCacheNames.SALES_SUMMARY,
            Duration.ofMinutes(5), Duration.ofMinutes(30))
    );
  }
}
