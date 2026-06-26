package com.tchalanet.server.features.ops.cache;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

class CacheOpsControllerTest {

  @Test
  @DisplayName("clears the plans cache group without requiring the UI to know cache names")
  void clearsPlansGroup() {
    var manager = new ConcurrentMapCacheManager(
        "catalog:plan:active_plans",
        "catalog:plan:plan_by_code",
        "catalog:plan:plan_by_id");
    var controller = new CacheOpsController(manager);

    var response = controller.clearCacheGroup("plans", "refresh plan catalog");

    assertThat(response.data().group()).isEqualTo("plans");
    assertThat(response.data().cleared()).containsExactlyInAnyOrder(
        "catalog:plan:active_plans",
        "catalog:plan:plan_by_code",
        "catalog:plan:plan_by_id");
    assertThat(response.data().missing()).isEmpty();
  }

  @Test
  @DisplayName("reports missing caches for a partially configured group")
  void reportsMissingGroupCaches() {
    var manager = new ConcurrentMapCacheManager("catalog:plan:active_plans");
    var controller = new CacheOpsController(manager);

    var response = controller.clearCacheGroup("plans", "refresh plan catalog");

    assertThat(response.data().cleared()).containsExactly("catalog:plan:active_plans");
    assertThat(response.data().missing()).containsExactlyInAnyOrder(
        "catalog:plan:plan_by_code",
        "catalog:plan:plan_by_id");
  }
}
