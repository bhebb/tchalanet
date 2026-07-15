package com.tchalanet.server.features.pagemodel.dynamic.providers.platformadmin;

import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.common.web.paging.TchSearchQuery;
import com.tchalanet.server.features.pagemodel.contract.ActionItem;
import com.tchalanet.server.features.pagemodel.contract.QuickActionsPayload;
import com.tchalanet.server.platform.contactrequest.api.ContactRequestAdminApi;
import com.tchalanet.server.platform.contactrequest.api.ContactRequestStatus;
import com.tchalanet.server.platform.contactrequest.api.model.ContactRequestSummaryView;
import com.tchalanet.server.platform.notification.api.NotificationApi;
import com.tchalanet.server.platform.notification.api.model.NotificationActorType;
import com.tchalanet.server.platform.notification.api.model.NotificationSeverity;
import com.tchalanet.server.platform.notification.api.model.NotificationStatus;
import com.tchalanet.server.platform.notification.api.model.view.NotificationItemView;
import com.tchalanet.server.platform.ops.api.OpsServiceResourceItem;
import com.tchalanet.server.platform.ops.api.PlatformHealthProbe;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PlatformAdminOpsDashboardPayloadAssembler {

  private final ObjectProvider<PlatformHealthProbe> healthProbeProvider;
  private final ObjectProvider<OpsResourceMetricsProvider> resourceMetricsProvider;
  private final ObjectProvider<NotificationApi> notificationApiProvider;
  private final ObjectProvider<ContactRequestAdminApi> contactRequestAdminApiProvider;

  public Payload assemble(TchRequestContext ctx) {
    DashboardTiming timing = DashboardTiming.start();
    Payload payload =
        new Payload(
            timing.record("health", this::buildHealth),
            buildSchedulerSummary(),
            timing.record("resources", this::buildResourceSummary),
            timing.record("notifications", () -> buildAppNotifications(ctx)),
            timing.record("contactRequests", this::buildContactRequests),
            timing.record("quickActions", this::buildQuickActions));
    timing.logPlatformOps(ctx);
    return payload;
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
      long upCount =
          components.values().stream().filter(status -> "UP".equalsIgnoreCase(status)).count();
      return new PlatformHealthPayload(global, components, upCount + "/" + components.size());
    } catch (RuntimeException e) {
      return new PlatformHealthPayload("UNKNOWN", Map.of(), "0/0");
    }
  }

  private QuickActionsPayload buildQuickActions() {
    return new QuickActionsPayload(
        java.util.List.of(
            new ActionItem(
                "DRAW_RESULTS",
                "quickaction.platform.draw_results",
                "fact_check",
                "/app/platform/ops/draw-results"),
            new ActionItem(
                "BATCH", "quickaction.platform.batch", "schedule", "/app/platform/ops/batch"),
            new ActionItem(
                "CACHE", "quickaction.platform.cache", "cached", "/app/platform/ops/cache"),
            new ActionItem(
                "IDENTITY_SYNC",
                "quickaction.platform.identity_sync",
                "sync",
                "/app/platform/ops/identity-sync"),
            new ActionItem(
                "AUDIT",
                "quickaction.platform.audit",
                "assignment_turned_in",
                "/app/platform/ops/audit")));
  }

  private OpsResourceSummaryPayload buildResourceSummary() {
    OpsResourceMetricsProvider provider = resourceMetricsProvider.getIfAvailable();
    if (provider == null) {
      return new OpsResourceSummaryPayload(
          java.time.Instant.now().toString(), 0, 0, java.util.List.of());
    }
    return provider.snapshot();
  }

  private OpsSchedulerSummaryPayload buildSchedulerSummary() {
    return new OpsSchedulerSummaryPayload(Instant.now().toString(), 0, 0, 0, 0, 0, false, List.of());
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
      var unreadCount =
          notificationApi.countUnread(
              NotificationActorType.APP_USER, ctx.userId().value(), ctx.userId(), roleCode);
      TchPage<NotificationItemView> notificationPage =
          notificationApi.listMyNotifications(
              NotificationActorType.APP_USER,
              ctx.userId().value(),
              ctx.userId(),
              roleCode,
              Optional.of(NotificationStatus.PUBLISHED),
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              TchSearchQuery.empty(),
              new TchPageRequest(PageRequest.of(0, 5, Sort.by(Sort.Order.desc("createdAt")))));
      List<NotificationItemView> notifications =
          notificationPage != null && notificationPage.items() != null
              ? notificationPage.items().stream()
                  .filter(PlatformAdminOpsDashboardPayloadAssembler::isUnread)
                  .toList()
              : List.of();
      long criticalCount =
          notifications.stream()
              .filter(notification -> notification.severity() == NotificationSeverity.CRITICAL)
              .count();
      return new OpsAlertPayload(
          unreadCount != null ? unreadCount.unreadCount() : notifications.size(),
          criticalCount,
          notifications.stream()
              .map(
                  notification ->
                      new OpsAlertItem(
                          notification.id() != null ? notification.id().value().toString() : "",
                          firstNonBlank(
                              notification.titleText(), notification.titleKey(), "Notification"),
                          firstNonBlank(notification.messageText(), notification.messageKey(), ""),
                          notification.severity() != null
                              ? mapNotificationSeverity(notification.severity().name())
                              : "INFO"))
              .toList());
    } catch (RuntimeException e) {
      return new OpsAlertPayload(
          0,
          0,
          List.of(
              new OpsAlertItem(
                  "notifications-unavailable",
                  "Notifications indisponibles",
                  "Le dashboard continue sans bloquer la page.",
                  "WARN")));
    }
  }

  private static boolean isUnread(NotificationItemView notification) {
    return notification != null
        && notification.readAt() == null
        && notification.archivedAt() == null;
  }

  private OpsAlertPayload buildContactRequests() {
    ContactRequestAdminApi contactApi = contactRequestAdminApiProvider.getIfAvailable();
    if (contactApi == null) {
      return new OpsAlertPayload(0, 0, List.of());
    }
    try {
      TchPage<ContactRequestSummaryView> page =
          contactApi.list(
              ContactRequestStatus.RECEIVED,
              null,
              new TchPageRequest(PageRequest.of(0, 5, Sort.by(Sort.Order.desc("createdAt")))));
      List<ContactRequestSummaryView> items =
          page != null && page.items() != null ? page.items() : List.of();
      return new OpsAlertPayload(
          page != null ? page.totalElements() : 0,
          0,
          items.stream()
              .map(
                  contact ->
                      new OpsAlertItem(
                          contact.id() != null ? contact.id().toString() : contact.reference(),
                          firstNonBlank(contact.fullName(), contact.reference(), "Contact"),
                          firstNonBlank(
                              contact.intent() != null ? contact.intent().name() : null,
                              contact.email(),
                              contact.phone(),
                              ""),
                          "INFO"))
              .toList());
    } catch (RuntimeException e) {
      return new OpsAlertPayload(
          0,
          0,
          List.of(
              new OpsAlertItem(
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

  public record Payload(
      PlatformHealthPayload health,
      OpsSchedulerSummaryPayload schedulerSummary,
      OpsResourceSummaryPayload resourceSummary,
      OpsAlertPayload appNotifications,
      OpsAlertPayload contactRequests,
      QuickActionsPayload quickActions) {}

  public record PlatformHealthPayload(
      String global, Map<String, String> components, String servicesUp) {}

  public record OpsResourceSummaryPayload(
      String generatedAt,
      long criticalCount,
      long warningCount,
      java.util.List<OpsServiceResourceItem> services) {}

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

  public record OpsAlertPayload(long totalCount, long criticalCount, List<OpsAlertItem> items) {}

  public record OpsAlertItem(String id, String title, String message, String severity) {}

  private static final class DashboardTiming {
    private final long startedAt = System.nanoTime();
    private final Map<String, Long> durationsMs = new LinkedHashMap<>();

    static DashboardTiming start() {
      return new DashboardTiming();
    }

    <T> T record(String name, Supplier<T> supplier) {
      long before = System.nanoTime();
      try {
        return supplier.get();
      } finally {
        durationsMs.put(name + "Ms", elapsedMs(before));
      }
    }

    void logPlatformOps(TchRequestContext ctx) {
      log.warn(
          "dashboard_timing surface=platform_admin_ops totalMs={} userId={} blocks={}",
          elapsedMs(startedAt),
          ctx != null && ctx.userId() != null ? ctx.userId().value() : null,
          durationsMs);
    }

    private static long elapsedMs(long startedAt) {
      return (System.nanoTime() - startedAt) / 1_000_000L;
    }
  }
}
