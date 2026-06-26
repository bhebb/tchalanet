package com.tchalanet.server.app.ops;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

class CacheOpsResourceContributorTest {

  @Test
  @DisplayName("reports plan caches as ok when critical regions are resolvable")
  void reportsPlanCachesOk() {
    var manager = new ConcurrentMapCacheManager(
        "catalog:plan:active_plans",
        "catalog:plan:plan_by_code",
        "catalog:plan:plan_by_id");

    var item = new CacheOpsResourceContributor(manager).services().getFirst();

    assertThat(item.status()).isEqualTo("OK");
    assertThat(item.severity()).isEqualTo("OK");
  }

  @Test
  @DisplayName("reports plan caches as critical when one critical region is missing")
  void reportsPlanCachesCritical() {
    var manager = new ConcurrentMapCacheManager("catalog:plan:active_plans");

    var item = new CacheOpsResourceContributor(manager).services().getFirst();

    assertThat(item.status()).isEqualTo("MISSING");
    assertThat(item.severity()).isEqualTo("CRITICAL");
    assertThat(item.message()).contains("catalog:plan:plan_by_code", "catalog:plan:plan_by_id");
  }
}
