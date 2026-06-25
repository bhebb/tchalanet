package com.tchalanet.server.features.pagemodel.dynamic.providers.platformadmin;

import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.job.gate.BatchGate;
import com.tchalanet.server.common.job.registry.RegisteredJob;
import com.tchalanet.server.common.job.registry.TchJobRegistry;
import com.tchalanet.server.features.pagemodel.contract.ActionItem;
import com.tchalanet.server.features.pagemodel.contract.QuickActionsPayload;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageRequest;
    import com.tchalanet.server.platform.contactrequest.api.ContactRequestAdminApi;
import com.tchalanet.server.platform.contactrequest.api.ContactRequestStatus;
import com.tchalanet.server.platform.contactrequest.api.model.ContactRequestSummaryView;
import com.tchalanet.server.platform.notification.api.NotificationApi;
import com.tchalanet.server.platform.notification.api.model.NotificationStatus;
import com.tchalanet.server.platform.notification.api.model.request.GetNotificationSummaryRequest;
import com.tchalanet.server.platform.notification.api.model.request.ListNotificationsRequest;
import com.tchalanet.server.platform.notification.api.model.view.NotificationItemView;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PlatformAdminOpsDashboardPayloadAssembler {

  private final ObjectProvider<PlatformHealthProbe> healthProbeProvider;
  private final ObjectProvider<OpsResourceMetricsProvider> resourceMetricsProvider;
  private final ObjectProvider<TchJobRegistry> jobRegistryProvider;
  private final ObjectProvider<BatchGate> batchGateProvider;
  private final ObjectProvider<OpsSchedulerHistoryProvider> schedulerHistoryProvider;
  private final ObjectProvider<NotificationApi> notificationApiProvider;
  private final ObjectProvider<ContactRequestAdminApi> contactRequestAdminApiProvider;

  public Payload assemble(TchRequestContext ctx) {
    return new Payload(
        buildHealth(),
        buildSchedulerSummary(),
        buildResourceSummary(),
        buildAppNotifications(ctx),
        buildContactRequests(),
        buildQuickActions());
  }

  @SuppressWarnings("unchecked")
  private PlatformHealthPayload buildHealth() {
    PlatformHealthProbe probe = healthProbeProvider.getIfAvailable();
    if (probe == null) {
      return new PlatformHealthPayload("UNKNOWN", Map.of(), "0/0");
    }
    try {
      Map<String, Object> snapshot = probe.snapshot();
      if (snapshot == null) {
        return new PlatformHealthPayload("UNKNOWN", Map.of(), "0/0");
      }
      String global = snapshot.getOrDefault("global", "UNKNOWN").toString();
      Object rawComponents = snapshot.get("components");
      Map<String, String> components = Map.of();
      if (rawComponents instanceof Map<?, ?> m) {
        var typed = new java.util.LinkedHashMap<String, String>();
        m.forEach((k, v) -> typed.put(String.valueOf(k), String.valueOf(v)));
        components = java.util.Collections.unmodifiableMap(typed);
      }
      long upCount = components.values().stream()
          .filter(status -> "UP".equalsIgnoreCase(status))
          .count();
      return new PlatformHealthPayload(global, components, upCount + "/" + components.size());
    } catch (RuntimeException e) {
      return new PlatformHealthPayload("UNKNOWN", Map.of(), "0/0");
    }
  }

  private QuickActionsPayload buildQuickActions() {
    return new QuickActionsPayload(java.util.List.of(
        new ActionItem("DRAW_RESULTS", "quickaction.platform.draw_results", "fact_check", "/app/platform/ops/draw-results"),
        new ActionItem("BATCH", "quickaction.platform.batch", "schedule", "/app/platform/ops/batch"),
        new ActionItem("CACHE", "quickaction.platform.cache", "cached", "/app/platform/ops/cache"),
        new ActionItem("IDENTITY_SYNC", "quickaction.platform.identity_sync", "sync", "/app/platform/ops/identity-sync"),
        new ActionItem("AUDIT", "quickaction.platform.audit", "assignment_turned_in", "/app/platform/ops/audit")));
  }

  private OpsResourceSummaryPayload buildResourceSummary() {
    OpsResourceMetricsProvider provider = resourceMetricsProvider.getIfAvailable();
    if (provider == null) {
      return new OpsResourceSummaryPayload(
          java.time.Instant.now().toString(),
          0,
          0,
          java.util.List.of());
    }
    return provider.snapshot();
  }

  private OpsSchedulerSummaryPayload buildSchedulerSummary() {
    TchJobRegistry registry = jobRegistryProvider.getIfAvailable();
    BatchGate gate = batchGateProvider.getIfAvailable();
    if (registry == null || gate == null) {
      return new OpsSchedulerSummaryPayload(
          Instant.now().toString(),
          0,
          0,
          0,
          0,
          0,
          false,
          List.of());
    }

    try {
      List<RegisteredJob> jobs = List.copyOf(registry.list());
      List<OpsSchedulerJobItem> disabled = jobs.stream()
          .filter(job -> !gate.enabled(job.jobKey(), null))
          .map(job -> new OpsSchedulerJobItem(
              job.jobKey().value(),
              job.displayName(),
              job.scope().name(),
              "DISABLED",
              "WARNING",
              "/app/platform/ops/batch",
              null))
          .toList();
      OpsSchedulerHistoryProvider.Snapshot history = buildSchedulerHistory();
      java.util.LinkedHashMap<String, OpsSchedulerJobItem> uniqueItems = new java.util.LinkedHashMap<>();
      disabled.forEach(item -> uniqueItems.put(item.jobKey(), item));
      history.items().forEach(item -> {
        OpsSchedulerJobItem mapped = new OpsSchedulerJobItem(
            item.jobKey(),
            item.displayName(),
            item.scope(),
            item.status(),
            item.severity(),
            item.detailsPath(),
            item.context());
        uniqueItems.merge(mapped.jobKey(), mapped, PlatformAdminOpsDashboardPayloadAssembler::pickMostSevere);
      });
      if (history.items().isEmpty()) {
        java.util.Set<String> disabledKeys = disabled.stream()
            .map(OpsSchedulerJobItem::jobKey)
            .collect(java.util.stream.Collectors.toSet());
        jobs.stream()
            .filter(job -> !disabledKeys.contains(job.jobKey().value()))
            .limit(5)
            .map(job -> new OpsSchedulerJobItem(
                job.jobKey().value(),
                job.displayName(),
                job.scope().name(),
                "REGISTERED",
                "OK",
                "/app/platform/ops/batch",
                null))
            .forEach(item -> uniqueItems.putIfAbsent(item.jobKey(), item));
      }
      return new OpsSchedulerSummaryPayload(
          Instant.now().toString(),
          jobs.size(),
          disabled.size(),
          history.failedCount(),
          history.staleCount(),
          history.neverRunCount(),
          history.historyAvailable(),
          List.copyOf(uniqueItems.values()));
    } catch (RuntimeException e) {
      return new OpsSchedulerSummaryPayload(
          Instant.now().toString(),
          0,
          0,
          0,
          0,
          0,
          false,
          List.of(new OpsSchedulerJobItem(
              "batch-runtime",
              "Batch runtime",
              "GLOBAL",
              "UNKNOWN",
              "WARNING",
              "/app/platform/ops/batch",
              null)));
    }
  }

  private OpsSchedulerHistoryProvider.Snapshot buildSchedulerHistory() {
    OpsSchedulerHistoryProvider provider = schedulerHistoryProvider.getIfAvailable();
    if (provider == null) {
      return new OpsSchedulerHistoryProvider.Snapshot(0, 0, 0, false, List.of());
    }
    try {
      return provider.snapshot();
    } catch (RuntimeException e) {
      return new OpsSchedulerHistoryProvider.Snapshot(
          0,
          0,
          0,
          false,
          List.of(new OpsSchedulerHistoryProvider.Item(
              "batch-history",
              "Batch history",
              "GLOBAL",
              "UNKNOWN",
              "WARNING",
              "/app/platform/ops/batch",
              null)));
    }
  }

  private OpsAlertPayload buildAppNotifications(TchRequestContext ctx) {
    if (ctx == null || ctx.userId() == null) {
      return new OpsAlertPayload(0, 0, List.of());
    }
    NotificationApi notificationApi = notificationApiProvider.getIfAvailable();
    if (notificationApi == null) {
      return new OpsAlertPayload(0, 0, List.of());
    }
    String roleCode = ctx.currentRole() != null ? ctx.currentRole().name() : null;
    try {
      var summary = notificationApi.getNotificationSummary(
          new GetNotificationSummaryRequest(ctx.userId(), roleCode));
      List<NotificationItemView> notifications = notificationApi.listNotifications(new ListNotificationsRequest(
          ctx.userId(),
          roleCode,
          Optional.of(NotificationStatus.UNREAD),
          Optional.empty(),
          Optional.empty(),
          Optional.empty(),
          new TchPageRequest(PageRequest.of(0, 5, Sort.by(Sort.Order.desc("createdAt"))))));
      return new OpsAlertPayload(
          summary != null ? summary.unreadCount() : 0,
          summary != null ? summary.criticalCount() : 0,
          notifications.stream()
              .map(notification -> new OpsAlertItem(
                  notification.id() != null ? notification.id().value().toString() : "",
                  firstNonBlank(notification.titleText(), notification.titleKey(), "Notification"),
                  firstNonBlank(notification.messageText(), notification.messageKey(), ""),
                  notification.severity() != null ? mapNotificationSeverity(notification.severity().name()) : "INFO"))
              .toList());
    } catch (RuntimeException e) {
      return new OpsAlertPayload(0, 0, List.of(new OpsAlertItem(
          "notifications-unavailable",
          "Notifications indisponibles",
          "Le dashboard continue sans bloquer la page.",
          "WARN")));
    }
  }

  private OpsAlertPayload buildContactRequests() {
    ContactRequestAdminApi contactApi = contactRequestAdminApiProvider.getIfAvailable();
    if (contactApi == null) {
      return new OpsAlertPayload(0, 0, List.of());
    }
    try {
      TchPage<ContactRequestSummaryView> page = contactApi.list(
          ContactRequestStatus.RECEIVED,
          null,
          new TchPageRequest(PageRequest.of(0, 5, Sort.by(Sort.Order.desc("createdAt")))));
      List<ContactRequestSummaryView> items = page != null && page.items() != null ? page.items() : List.of();
      return new OpsAlertPayload(
          page != null ? page.totalElements() : 0,
          0,
          items.stream()
              .map(contact -> new OpsAlertItem(
                  contact.id() != null ? contact.id().toString() : contact.reference(),
                  firstNonBlank(contact.fullName(), contact.reference(), "Contact"),
                  firstNonBlank(contact.intent() != null ? contact.intent().name() : null, contact.email(), contact.phone(), ""),
                  "INFO"))
              .toList());
    } catch (RuntimeException e) {
      return new OpsAlertPayload(0, 0, List.of(new OpsAlertItem(
          "contacts-unavailable",
          "Contacts indisponibles",
          "Le dashboard continue sans bloquer la page.",
          "WARN")));
    }
  }

  private static String firstNonBlank(String... values) {
    if (values == null) return "";
    for (String value : values) {
      if (value != null && !value.isBlank()) return value;
    }
    return "";
  }

  private static String mapNotificationSeverity(String severity) {
    return switch (severity) {
      case "ERROR", "CRITICAL" -> "ERROR";
      case "WARNING" -> "WARN";
      default -> "INFO";
    };
  }

  private static OpsSchedulerJobItem pickMostSevere(OpsSchedulerJobItem left, OpsSchedulerJobItem right) {
    return severityRank(right.severity()) < severityRank(left.severity()) ? right : left;
  }

  private static int severityRank(String severity) {
    return switch (severity) {
      case "CRITICAL" -> 0;
      case "WARNING" -> 1;
      case "OK" -> 2;
      default -> 1;
    };
  }

  public record Payload(
      PlatformHealthPayload health,
      OpsSchedulerSummaryPayload schedulerSummary,
      OpsResourceSummaryPayload resourceSummary,
      OpsAlertPayload appNotifications,
      OpsAlertPayload contactRequests,
      QuickActionsPayload quickActions) {}

  public record PlatformHealthPayload(
      String global,
      Map<String, String> components,
      String servicesUp) {}

  public record OpsResourceSummaryPayload(
      String generatedAt,
      long criticalCount,
      long warningCount,
      java.util.List<OpsServiceResourceItem> services) {}

  public record OpsServiceResourceItem(
      String serviceKey,
      String displayName,
      String status,
      Integer memoryUsedMb,
      Integer memoryLimitMb,
      Integer memoryPercent,
      Double cpuPercent,
      Integer restartCount,
      Boolean oomKilled,
      String lastRestartAt,
      String severity,
      String message,
      String detailsPath,
      Integer sizeMb,
      Integer indexSizeMb,
      Integer tableCount) {}

  public record OpsSchedulerSummaryPayload(
      String generatedAt,
      long registeredCount,
      long disabledGateCount,
      long failedCount,
      long staleCount,
      long neverRunCount,
      boolean historyAvailable,
      List<OpsSchedulerJobItem> items) {}

  public record OpsSchedulerJobItem(
      String jobKey,
      String displayName,
      String scope,
      String status,
      String severity,
      String detailsPath,
      String context) {}

  public record OpsAlertPayload(
      long totalCount,
      long criticalCount,
      List<OpsAlertItem> items) {}

  public record OpsAlertItem(
      String id,
      String title,
      String message,
      String severity) {}
}
