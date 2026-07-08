package com.tchalanet.server.features.ops.cache;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.common.cache.CacheToggle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

class CacheOpsControllerTest {

  @Test
  @DisplayName("clears the pricing cache group without requiring the UI to know cache names")
  void clearsPricingGroup() {
    var manager = new ConcurrentMapCacheManager(
        "core:pricing:tenant_odds_list",
        "core:pricing:tenant_odds_by_variant");
    var controller = new CacheOpsController(manager, new CacheToggle());

    var response = controller.clearCacheGroup("pricing", "refresh odds");

    assertThat(response.data().group()).isEqualTo("pricing");
    assertThat(response.data().cleared()).containsExactlyInAnyOrder(
        "core:pricing:tenant_odds_list",
        "core:pricing:tenant_odds_by_variant");
    assertThat(response.data().missing()).isEmpty();
  }

  @Test
  @DisplayName("reports missing caches for a partially configured group")
  void reportsMissingGroupCaches() {
    var manager = new ConcurrentMapCacheManager("core:pricing:tenant_odds_list");
    var controller = new CacheOpsController(manager, new CacheToggle());

    var response = controller.clearCacheGroup("pricing", "refresh odds");

    assertThat(response.data().cleared()).containsExactly("core:pricing:tenant_odds_list");
    assertThat(response.data().missing()).containsExactly("core:pricing:tenant_odds_by_variant");
  }

  @Test
  @DisplayName("disable/enable toggles a cache at runtime and lists disabled caches")
  void disableEnableToggle() {
    var toggle = new CacheToggle();
    var controller = new CacheOpsController(new ConcurrentMapCacheManager(), toggle);

    controller.disableCache("catalog:game:active_games", "bad data");
    assertThat(toggle.isEnabled("catalog:game:active_games")).isFalse();
    assertThat(controller.listDisabledCaches().data()).containsExactly("catalog:game:active_games");

    controller.enableCache("catalog:game:active_games", null);
    assertThat(toggle.isEnabled("catalog:game:active_games")).isTrue();
    assertThat(controller.listDisabledCaches().data()).isEmpty();
  }
}
