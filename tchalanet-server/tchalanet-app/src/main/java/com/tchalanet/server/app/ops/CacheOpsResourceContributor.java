package com.tchalanet.server.app.ops;

import com.tchalanet.server.features.pagemodel.dynamic.providers.platformadmin.OpsResourceContributor;
import com.tchalanet.server.features.pagemodel.dynamic.providers.platformadmin.PlatformAdminOpsDashboardPayloadAssembler;
import java.util.List;
import org.springframework.cache.CacheManager;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(110)
public class CacheOpsResourceContributor implements OpsResourceContributor {

  private static final List<String> CRITICAL_PLAN_CACHES = List.of(
      "catalog:plan:active_plans",
      "catalog:plan:plan_by_code",
      "catalog:plan:plan_by_id");

  private final CacheManager cacheManager;

  public CacheOpsResourceContributor(CacheManager cacheManager) {
    this.cacheManager = cacheManager;
  }

  @Override
  public List<PlatformAdminOpsDashboardPayloadAssembler.OpsServiceResourceItem> services() {
    int cacheRegionCount = cacheManager.getCacheNames().size();
    List<String> unresolved = CRITICAL_PLAN_CACHES.stream()
        .filter(cacheName -> cacheManager.getCache(cacheName) == null)
        .toList();

    String severity = unresolved.isEmpty() ? "OK" : "CRITICAL";
    String status = unresolved.isEmpty() ? "OK" : "MISSING";
    String message = unresolved.isEmpty()
        ? "Critical plan cache regions are resolvable. " + cacheRegionCount + " cache regions visible."
        : "Critical plan cache regions are not resolvable: " + String.join(", ", unresolved) + ".";

    return List.of(new PlatformAdminOpsDashboardPayloadAssembler.OpsServiceResourceItem(
        "runtime:caches",
        "Cache manager",
        status,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        severity,
        message,
        "/app/platform/ops/cache",
        null,
        null,
        null));
  }
}
