package com.tchalanet.server.features.pagemodel.dynamic.providers.platformadmin;

import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.features.pagemodel.contract.ActionItem;
import com.tchalanet.server.features.pagemodel.contract.QuickActionsPayload;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PlatformAdminOpsDashboardPayloadAssembler {

  private final ObjectProvider<PlatformHealthProbe> healthProbeProvider;

  public Payload assemble(TchRequestContext ctx) {
    return new Payload(buildHealth(), buildQuickActions());
  }

  @SuppressWarnings("unchecked")
  private PlatformHealthPayload buildHealth() {
    PlatformHealthProbe probe = healthProbeProvider.getIfAvailable();
    if (probe == null) {
      return new PlatformHealthPayload("UNKNOWN", Map.of());
    }
    try {
      Map<String, Object> snapshot = probe.snapshot();
      if (snapshot == null) {
        return new PlatformHealthPayload("UNKNOWN", Map.of());
      }
      String global = snapshot.getOrDefault("global", "UNKNOWN").toString();
      Object rawComponents = snapshot.get("components");
      Map<String, String> components = Map.of();
      if (rawComponents instanceof Map<?, ?> m) {
        var typed = new java.util.LinkedHashMap<String, String>();
        m.forEach((k, v) -> typed.put(String.valueOf(k), String.valueOf(v)));
        components = java.util.Collections.unmodifiableMap(typed);
      }
      return new PlatformHealthPayload(global, components);
    } catch (RuntimeException e) {
      return new PlatformHealthPayload("UNKNOWN", Map.of());
    }
  }

  private QuickActionsPayload buildQuickActions() {
    return new QuickActionsPayload(java.util.List.of(
        new ActionItem("DRAW_RESULTS", "quickaction.platform.draw_results", "fact_check", "/app/platform/ops/draw-results"),
        new ActionItem("BATCH", "quickaction.platform.batch", "schedule", "/app/platform/ops/batch"),
        new ActionItem("CACHE", "quickaction.platform.cache", "cached", "/app/platform/ops/cache"),
        new ActionItem("AUDIT", "quickaction.platform.audit", "assignment_turned_in", "/app/platform/ops/audit")));
  }

  public record Payload(
      PlatformHealthPayload health,
      QuickActionsPayload quickActions) {}

  public record PlatformHealthPayload(
      String global,
      Map<String, String> components) {}
}
