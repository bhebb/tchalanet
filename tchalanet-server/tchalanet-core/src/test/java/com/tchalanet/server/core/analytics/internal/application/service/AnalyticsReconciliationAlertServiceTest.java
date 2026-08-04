package com.tchalanet.server.core.analytics.internal.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.tchalanet.server.common.json.utils.JsonUtils;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.analytics.api.model.AnalyticsReconciliationMode;
import com.tchalanet.server.core.analytics.api.model.AnalyticsReconciliationResult;
import com.tchalanet.server.core.analytics.api.model.AnalyticsReconciliationStatus;
import com.tchalanet.server.core.analytics.internal.infra.persistence.AnalyticsVisibilityOverrideEntity;
import com.tchalanet.server.core.analytics.internal.infra.persistence.AnalyticsVisibilityOverrideRepository;
import com.tchalanet.server.platform.audit.api.AuditApi;
import com.tchalanet.server.platform.notification.api.NotificationApi;
import com.tchalanet.server.platform.notification.api.model.request.CreateNotificationRequest;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AnalyticsReconciliationAlertServiceTest {

  private static final TenantId TENANT_ID =
      TenantId.of(UUID.fromString("10000000-0000-0000-0000-000000000001"));
  private static final LocalDate FROM = LocalDate.of(2026, 8, 1);
  private static final LocalDate TO = LocalDate.of(2026, 8, 3);
  private static final Instant NOW = Instant.parse("2026-08-04T12:00:00Z");

  @Test
  void mismatchDisablesTheTenantScopeAuditsAndNotifiesWithStableDedupeKey() {
    var repository = mock(AnalyticsVisibilityOverrideRepository.class);
    var auditApi = mock(AuditApi.class);
    var notificationApi = mock(NotificationApi.class);
    var service = service(repository, auditApi, notificationApi);
    var result =
        new AnalyticsReconciliationResult(
            UUID.fromString("20000000-0000-0000-0000-000000000001"),
            TENANT_ID,
            FROM,
            TO,
            AnalyticsReconciliationMode.VALIDATE,
            AnalyticsReconciliationStatus.MISMATCH,
            List.of(),
            NOW);

    service.onReconciliationMismatch(result);

    var entity = ArgumentCaptor.forClass(AnalyticsVisibilityOverrideEntity.class);
    verify(repository).save(entity.capture());
    assertThat(entity.getValue().getScopeType().name()).isEqualTo("TENANT");
    assertThat(entity.getValue().getTenantId()).isEqualTo(TENANT_ID.value());
    assertThat(entity.getValue().getReason())
        .startsWith(AnalyticsReconciliationAlertService.AUTOMATIC_REASON_PREFIX);
    verify(auditApi).logAuditEvent(any());

    var notification = ArgumentCaptor.forClass(CreateNotificationRequest.class);
    verify(notificationApi).createNotification(notification.capture());
    assertThat(notification.getValue().dedupeKey())
        .isEqualTo(
            "analytics.reconciliation.alert:%s:%s:%s:RECONCILIATION_MISMATCH"
                .formatted(TENANT_ID.value(), FROM, TO));
  }

  @Test
  void successfulRebuildRemovesOnlyAutomaticScopeOverride() {
    var repository = mock(AnalyticsVisibilityOverrideRepository.class);
    var service = service(repository, mock(AuditApi.class), mock(NotificationApi.class));
    var result =
        new AnalyticsReconciliationResult(
            UUID.randomUUID(),
            TENANT_ID,
            FROM,
            TO,
            AnalyticsReconciliationMode.REBUILD_AND_VALIDATE,
            AnalyticsReconciliationStatus.SUCCESS,
            List.of(),
            NOW);

    service.clearAutomaticUnavailable(result);

    verify(repository)
        .deleteAutomaticExactScope(
            eq(TENANT_ID.value()), eq(FROM), eq(TO), eq("automatic analytics alert: %"));
  }

  private static AnalyticsReconciliationAlertService service(
      AnalyticsVisibilityOverrideRepository repository,
      AuditApi auditApi,
      NotificationApi notificationApi) {
    return new AnalyticsReconciliationAlertService(
        repository,
        auditApi,
        notificationApi,
        mock(JsonUtils.class),
        Clock.fixed(NOW, ZoneOffset.UTC));
  }
}
