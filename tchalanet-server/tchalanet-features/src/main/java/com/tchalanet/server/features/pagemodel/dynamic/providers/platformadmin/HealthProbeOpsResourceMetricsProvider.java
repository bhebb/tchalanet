package com.tchalanet.server.features.pagemodel.dynamic.providers.platformadmin;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class HealthProbeOpsResourceMetricsProvider implements OpsResourceMetricsProvider {

  private final ObjectProvider<PlatformHealthProbe> healthProbeProvider;
  private final ObjectProvider<OpsResourceContributor> resourceContributors;

  @Override
  public PlatformAdminOpsDashboardPayloadAssembler.OpsResourceSummaryPayload snapshot() {
    PlatformHealthProbe probe = healthProbeProvider.getIfAvailable();
    if (probe == null) {
      return withContributors(emptyServices("Observability provider unavailable."));
    }

    try {
      Map<String, Object> snapshot = probe.snapshot();
      if (snapshot == null) {
        return withContributors(emptyServices("Health snapshot unavailable."));
      }

      List<PlatformAdminOpsDashboardPayloadAssembler.OpsServiceResourceItem> services =
          new ArrayList<>(mapServices(snapshot));
      return withContributors(services);
    } catch (RuntimeException e) {
      return withContributors(emptyServices("Resource metrics could not be collected."));
    }
  }

  private PlatformAdminOpsDashboardPayloadAssembler.OpsResourceSummaryPayload withContributors(
      List<PlatformAdminOpsDashboardPayloadAssembler.OpsServiceResourceItem> baseServices) {
    List<PlatformAdminOpsDashboardPayloadAssembler.OpsServiceResourceItem> services =
        new ArrayList<>(baseServices);
    resourceContributors.orderedStream().forEach(contributor -> services.addAll(safeServices(contributor)));
    long criticalCount = services.stream().filter(item -> "CRITICAL".equals(item.severity())).count();
    long warningCount = services.stream().filter(item -> "WARNING".equals(item.severity())).count();
    return new PlatformAdminOpsDashboardPayloadAssembler.OpsResourceSummaryPayload(
        Instant.now().toString(),
        criticalCount,
        warningCount,
        List.copyOf(services));
  }

  private static List<PlatformAdminOpsDashboardPayloadAssembler.OpsServiceResourceItem> safeServices(
      OpsResourceContributor contributor) {
    try {
      List<PlatformAdminOpsDashboardPayloadAssembler.OpsServiceResourceItem> services = contributor.services();
      return services == null ? List.of() : services;
    } catch (RuntimeException e) {
      return List.of(new PlatformAdminOpsDashboardPayloadAssembler.OpsServiceResourceItem(
          "ops-resource-contributor",
          "Ops resource contributor",
          "UNKNOWN",
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          "WARNING",
          "Resource contributor could not be collected.",
          "/app/platform/ops/resources",
          null,
          null,
          null));
    }
  }

  private static List<PlatformAdminOpsDashboardPayloadAssembler.OpsServiceResourceItem> emptyServices(String message) {
    return List.of(new PlatformAdminOpsDashboardPayloadAssembler.OpsServiceResourceItem(
            "observability",
            "Observability",
            "UNKNOWN",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            "WARNING",
            message,
            "/app/platform/ops/resources",
            null,
            null,
            null));
  }

  private static List<PlatformAdminOpsDashboardPayloadAssembler.OpsServiceResourceItem> mapServices(
      Map<String, Object> snapshot) {
    List<PlatformAdminOpsDashboardPayloadAssembler.OpsServiceResourceItem> services = new ArrayList<>();
    services.add(mapService("api", "API service", snapshot.getOrDefault("global", "UNKNOWN"), snapshot.get("appVersion")));

    Object rawComponents = snapshot.get("components");
    if (rawComponents instanceof Map<?, ?> components) {
      components.forEach((key, value) -> services.add(mapService(String.valueOf(key), displayName(key), value, null)));
    }

    return List.copyOf(services);
  }

  private static PlatformAdminOpsDashboardPayloadAssembler.OpsServiceResourceItem mapService(
      String key,
      String displayName,
      Object statusValue,
      Object versionValue) {
    String status = normalizeStatus(statusValue);
    String severity = severity(status);
    String version = versionValue == null ? "" : String.valueOf(versionValue).trim();
    String versionLabel = version.isBlank() ? "" : " v" + version;
    String message = switch (severity) {
      case "CRITICAL" -> displayName + versionLabel + " is down.";
      case "WARNING" -> displayName + versionLabel + " status is " + status + ".";
      default -> displayName + versionLabel + " is healthy.";
    };
    return new PlatformAdminOpsDashboardPayloadAssembler.OpsServiceResourceItem(
        key,
        displayName,
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
        "/app/platform/ops/resources",
        null,
        null,
        null);
  }

  private static String normalizeStatus(Object value) {
    String raw = value == null ? "UNKNOWN" : String.valueOf(value).trim().toUpperCase(Locale.ROOT);
    return switch (raw) {
      case "UP" -> "UP";
      case "DOWN" -> "DOWN";
      case "DEGRADED", "OUT_OF_SERVICE" -> "DEGRADED";
      default -> "UNKNOWN";
    };
  }

  private static String severity(String status) {
    return switch (status) {
      case "UP" -> "OK";
      case "DOWN" -> "CRITICAL";
      default -> "WARNING";
    };
  }

  private static String displayName(Object key) {
    String normalized = String.valueOf(key).trim().toLowerCase(Locale.ROOT);
    if ("db".equals(normalized) || "postgres".equals(normalized) || "postgresql".equals(normalized)) {
      return "Postgres";
    }
    if ("redis".equals(normalized)) {
      return "Redis";
    }
    if ("edge".equals(normalized) || "edge-service".equals(normalized) || "edge_service".equals(normalized)) {
      return "Edge service";
    }
    String raw = String.valueOf(key).replace('_', ' ').replace('-', ' ').trim();
    if (raw.isBlank()) return "Service";
    return raw.substring(0, 1).toUpperCase(Locale.ROOT) + raw.substring(1);
  }
}
