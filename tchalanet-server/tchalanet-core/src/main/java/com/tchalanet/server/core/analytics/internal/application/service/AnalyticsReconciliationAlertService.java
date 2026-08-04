package com.tchalanet.server.core.analytics.internal.application.service;

import com.tchalanet.server.common.json.utils.JsonUtils;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.core.analytics.api.model.AnalyticsReconciliationResult;
import com.tchalanet.server.core.analytics.api.model.AnalyticsTrustScope;
import com.tchalanet.server.core.analytics.internal.infra.persistence.AnalyticsVisibilityOverrideEntity;
import com.tchalanet.server.core.analytics.internal.infra.persistence.AnalyticsVisibilityOverrideRepository;
import com.tchalanet.server.platform.audit.api.AuditApi;
import com.tchalanet.server.platform.audit.api.model.AuditAction;
import com.tchalanet.server.platform.audit.api.model.AuditEntityType;
import com.tchalanet.server.platform.audit.api.model.request.LogAuditEventRequest;
import com.tchalanet.server.platform.notification.api.NotificationApi;
import com.tchalanet.server.platform.notification.api.model.NotificationAudienceType;
import com.tchalanet.server.platform.notification.api.model.NotificationCategory;
import com.tchalanet.server.platform.notification.api.model.NotificationChannel;
import com.tchalanet.server.platform.notification.api.model.NotificationKind;
import com.tchalanet.server.platform.notification.api.model.NotificationSeverity;
import com.tchalanet.server.platform.notification.api.model.NotificationTranslationInput;
import com.tchalanet.server.platform.notification.api.model.request.CreateNotificationRequest;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;

/**
 * Persists analytics degradation and notifies platform operators outside the failed projection
 * transaction.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsReconciliationAlertService {

  static final String AUTOMATIC_REASON_PREFIX = "automatic analytics alert: ";
  private static final UUID SYSTEM_ACTOR_ID = new UUID(0L, 0L);

  private final AnalyticsVisibilityOverrideRepository visibilityOverrideRepository;
  private final AuditApi auditApi;
  private final NotificationApi notificationApi;
  private final JsonUtils jsonUtils;
  private final Clock clock;

  @TchTx(propagation = Propagation.REQUIRES_NEW)
  public void onReconciliationMismatch(AnalyticsReconciliationResult result) {
    var scope = AnalyticsTrustScope.tenant(result.tenantId(), result.from(), result.to());
    var details = new LinkedHashMap<String, Object>();
    details.put("alertType", "RECONCILIATION_MISMATCH");
    details.put("runId", result.runId().toString());
    details.put("tenantId", result.tenantId().value().toString());
    details.put("from", result.from().toString());
    details.put("to", result.to().toString());
    details.put("mismatchCount", result.mismatches().size());
    alert(scope, "reconciliation mismatch run=" + result.runId(), details);
  }

  @TchTx(propagation = Propagation.REQUIRES_NEW)
  public void onProjectionFailure(
      AnalyticsTrustScope scope, String projection, String eventId, Throwable failure) {
    var details = new LinkedHashMap<String, Object>();
    details.put("alertType", "PROJECTION_FAILURE");
    details.put("projection", projection);
    details.put("eventId", eventId);
    details.put("failureType", failure.getClass().getName());
    details.put("failureMessage", failure.getMessage() == null ? "" : failure.getMessage());
    alert(scope, "projection failure projection=" + projection, details);
  }

  @TchTx(propagation = Propagation.REQUIRES_NEW)
  public void clearAutomaticUnavailable(AnalyticsReconciliationResult result) {
    visibilityOverrideRepository.deleteAutomaticExactScope(
        result.tenantId().value(), result.from(), result.to(), AUTOMATIC_REASON_PREFIX + "%");
  }

  private void alert(AnalyticsTrustScope scope, String suffix, Map<String, Object> details) {
    var reason = AUTOMATIC_REASON_PREFIX + suffix;
    visibilityOverrideRepository.deleteAutomaticExactScope(
        scope.tenantId().value(), scope.from(), scope.to(), AUTOMATIC_REASON_PREFIX + "%");
    visibilityOverrideRepository.save(
        AnalyticsVisibilityOverrideEntity.builder()
            .scopeType(scope.type())
            .tenantId(scope.tenantId().value())
            .fromDate(scope.from())
            .toDate(scope.to())
            .reason(reason)
            .changedBy(SYSTEM_ACTOR_ID)
            .changedAt(clock.instant())
            .build());
    auditApi.logAuditEvent(
        new LogAuditEventRequest(
            AuditEntityType.SYSTEM,
            scope.tenantId().value().toString(),
            AuditAction.ANALYTICS_VISIBILITY_OVERRIDE,
            Map.copyOf(details),
            scope.tenantId().value()));
    try {
      sendNotification(scope, reason, details);
    } catch (RuntimeException exception) {
      log.warn(
          "analytics alert notification failed tenant={} from={} to={}",
          scope.tenantId(),
          scope.from(),
          scope.to(),
          exception);
    }
  }

  private void sendNotification(
      AnalyticsTrustScope scope, String reason, Map<String, Object> details) {
    String dedupeKey =
        "analytics.reconciliation.alert:%s:%s:%s:%s"
            .formatted(
                scope.tenantId().value(), scope.from(), scope.to(), details.get("alertType"));
    String message =
        "Les indicateurs analytics du tenant %s sont indisponibles du %s au %s: %s."
            .formatted(scope.tenantId().value(), scope.from(), scope.to(), reason);
    var translations =
        Map.of(
            "fr", new NotificationTranslationInput("Analytics indisponibles", message),
            "en",
                new NotificationTranslationInput(
                    "Analytics unavailable",
                    "Analytics for tenant %s are unavailable from %s to %s: %s."
                        .formatted(scope.tenantId().value(), scope.from(), scope.to(), reason)));
    var payload = new LinkedHashMap<>(details);
    payload.put("tenantId", scope.tenantId().value().toString());
    payload.put("from", scope.from().toString());
    payload.put("to", scope.to().toString());
    payload.put("dedupeKey", dedupeKey);
    notificationApi.createNotification(
        new CreateNotificationRequest(
            null,
            "ANALYTICS_RECONCILIATION_ALERT",
            scope.tenantId().value().toString(),
            dedupeKey,
            NotificationAudienceType.PLATFORM_ADMINS,
            Set.of(),
            NotificationSeverity.ERROR,
            NotificationKind.ACTION_REQUIRED,
            NotificationCategory.SYSTEM,
            null,
            null,
            "Analytics indisponibles",
            message,
            translations,
            jsonUtils.toJsonNode(payload),
            "ANALYTICS_RECONCILIATION",
            "/app/platform/ops/analytics/reconciliation",
            null,
            Set.of(NotificationChannel.WEB, NotificationChannel.SLACK)));
  }
}
