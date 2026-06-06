package com.tchalanet.server.core.pagemodel.api.dynamic;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Declares legal widget ids per (schemaVersion, source). Consumed at seed time
 * (template validation) and at resolution time (unknown widgetId → dynamic.error).
 *
 * V1 sources (per dashboard-overview-runtime-v1):
 *   json_file, public_home, public_draw_results,
 *   tenant_admin_dashboard, cashier_dashboard, platform_admin_dashboard
 */
public final class WidgetRegistry {

  public static final Set<String> ALLOWED_SOURCES_V1 = Set.of(
      "jsonFile",
      "public_home",
      "public_draw_results",
      "tenant_admin_dashboard",
      "cashier_dashboard",
      "platform_admin_dashboard"
  );

  private final Map<Key, Set<String>> widgets;

  private WidgetRegistry(Map<Key, Set<String>> widgets) {
    this.widgets = widgets;
  }

  public boolean isSourceAllowed(String source) {
    return ALLOWED_SOURCES_V1.contains(source);
  }

  public boolean isWidgetAllowed(int schemaVersion, String source, String widgetId) {
    Set<String> ids = widgets.get(new Key(schemaVersion, source));
    return ids != null && ids.contains(widgetId);
  }

  public Set<String> widgetsFor(int schemaVersion, String source) {
    Set<String> ids = widgets.get(new Key(schemaVersion, source));
    return ids == null ? Set.of() : Collections.unmodifiableSet(ids);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private final Map<Key, Set<String>> widgets = new HashMap<>();

    public Builder register(int schemaVersion, String source, String... widgetIds) {
      if (!ALLOWED_SOURCES_V1.contains(source)) {
        throw new IllegalArgumentException("Source not allowed in V1: " + source);
      }
      Key key = new Key(schemaVersion, source);
      Set<String> set = widgets.computeIfAbsent(key, k -> new HashSet<>());
      for (String id : widgetIds) {
        set.add(id);
      }
      return this;
    }

    public WidgetRegistry build() {
      return new WidgetRegistry(Map.copyOf(widgets));
    }
  }

  private record Key(int schemaVersion, String source) {}
}
