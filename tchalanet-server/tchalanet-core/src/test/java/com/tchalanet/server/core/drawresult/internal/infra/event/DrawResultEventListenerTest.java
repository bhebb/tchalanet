package com.tchalanet.server.core.drawresult.internal.infra.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.json.utils.JsonUtils;
import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.DrawResultId;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.ResultSlotId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.draw.api.DrawResultAffectedTenantApi;
import com.tchalanet.server.core.draw.api.query.DrawResultAffectedTenant;
import com.tchalanet.server.core.drawresult.api.event.GlobalDrawResultAvailableEvent;
import com.tchalanet.server.core.drawresult.api.event.GlobalDrawResultCorrectedEvent;
import com.tchalanet.server.core.drawresult.api.model.ResultSource;
import com.tchalanet.server.platform.idempotence.api.ProcessedEventPort;
import com.tchalanet.server.platform.notification.api.NotificationApi;
import com.tchalanet.server.platform.notification.api.model.NotificationAudienceType;
import com.tchalanet.server.platform.notification.api.model.NotificationChannel;
import com.tchalanet.server.platform.notification.api.model.request.CreateNotificationRequest;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class DrawResultEventListenerTest {

  @Test
  void resultAvailableCreatesTenantAdminNotificationsOnlyForAffectedTenants() {
    var processed = mock(ProcessedEventPort.class);
    when(processed.markProcessedIfAbsent(anyString(), any(UUID.class))).thenReturn(true);
    var notificationApi = mock(NotificationApi.class);
    var affectedTenantApi = mock(DrawResultAffectedTenantApi.class);
    var jsonUtils = mock(JsonUtils.class);
    when(jsonUtils.toJsonNode(any())).thenReturn(JsonUtils.emptyObject());

    var tenantA = TenantId.of(UUID.randomUUID());
    var tenantB = TenantId.of(UUID.randomUUID());
    var resultSlotId = ResultSlotId.of(UUID.randomUUID());
    var drawDate = LocalDate.of(2026, 7, 16);
    when(affectedTenantApi.listAffectedTenants(resultSlotId, drawDate))
        .thenReturn(
            List.of(
                affected(tenantA, "FL-EVE", "Florida Evening"),
                affected(tenantB, "FL-EVE", "Florida Evening")));

    var listener =
        new DrawResultEventListener(
            processed,
            mock(CommandBus.class),
            Clock.fixed(Instant.parse("2026-07-16T22:00:00Z"), ZoneOffset.UTC),
            notificationApi,
            affectedTenantApi,
            jsonUtils);

    var drawResultId = DrawResultId.of(UUID.randomUUID());
    listener.onGlobalDrawResultAvailable(
        new GlobalDrawResultAvailableEvent(
            EventId.of(UUID.randomUUID()),
            Instant.parse("2026-07-16T22:00:00Z"),
            null,
            resultSlotId,
            "FL_EVE",
            drawResultId,
            Instant.parse("2026-07-16T21:00:00Z"),
            drawDate,
            "FL",
            ResultSource.PROVIDER));

    var captor = ArgumentCaptor.forClass(CreateNotificationRequest.class);
    verify(notificationApi, org.mockito.Mockito.times(2)).createNotification(captor.capture());

    assertThat(captor.getAllValues())
        .extracting(CreateNotificationRequest::tenantId)
        .containsExactly(tenantA, tenantB);
    assertThat(captor.getAllValues())
        .allSatisfy(
            request -> {
              assertThat(request.audienceType()).isEqualTo(NotificationAudienceType.TENANT_ADMINS);
              assertThat(request.channels()).containsExactly(NotificationChannel.WEB);
              assertThat(request.dedupeKey())
                  .startsWith("drawresult.available:" + drawResultId.value());
              assertThat(request.targets()).isEmpty();
              assertThat(request.translations()).containsOnlyKeys("fr", "en", "ht");
              assertThat(request.translations().get("fr").title()).isEqualTo("Résultat disponible");
            });
  }

  @Test
  void resultCorrectedCreatesPlatformOpsAndAffectedTenantNotifications() {
    var processed = mock(ProcessedEventPort.class);
    when(processed.markProcessedIfAbsent(anyString(), any(UUID.class))).thenReturn(true);
    var notificationApi = mock(NotificationApi.class);
    var affectedTenantApi = mock(DrawResultAffectedTenantApi.class);
    var jsonUtils = mock(JsonUtils.class);
    when(jsonUtils.toJsonNode(any())).thenReturn(JsonUtils.emptyObject());
    var tenant = TenantId.of(UUID.randomUUID());
    var resultSlotId = ResultSlotId.of(UUID.randomUUID());
    var drawDate = LocalDate.of(2026, 7, 16);
    when(affectedTenantApi.listAffectedTenants(resultSlotId, drawDate))
        .thenReturn(List.of(affected(tenant, "FL-EVE", "Florida Evening")));

    var listener =
        new DrawResultEventListener(
            processed,
            mock(CommandBus.class),
            Clock.fixed(Instant.parse("2026-07-16T22:00:00Z"), ZoneOffset.UTC),
            notificationApi,
            affectedTenantApi,
            jsonUtils);

    var drawResultId = DrawResultId.of(UUID.randomUUID());
    listener.onGlobalDrawResultCorrected(
        new GlobalDrawResultCorrectedEvent(
            EventId.of(UUID.randomUUID()),
            Instant.parse("2026-07-16T22:00:00Z"),
            null,
            resultSlotId,
            "FL_EVE",
            drawResultId,
            Instant.parse("2026-07-16T21:00:00Z"),
            drawDate,
            "FL",
            ResultSource.MANUAL_OVERRIDE,
            "wrong result"));

    var captor = ArgumentCaptor.forClass(CreateNotificationRequest.class);
    verify(notificationApi, org.mockito.Mockito.times(2)).createNotification(captor.capture());

    var requests = captor.getAllValues();
    var opsRequest =
        requests.stream().filter(request -> request.tenantId() == null).findFirst().orElseThrow();
    assertThat(opsRequest.audienceType()).isEqualTo(NotificationAudienceType.PLATFORM_ADMINS);
    assertThat(opsRequest.dedupeKey())
        .isEqualTo("drawresult.corrected:%s:PLATFORM_ADMINS".formatted(drawResultId.value()));
    assertThat(opsRequest.channels()).containsExactly(NotificationChannel.WEB);
    assertThat(opsRequest.translations()).containsOnlyKeys("fr", "en", "ht");

    var tenantRequest =
        requests.stream()
            .filter(request -> tenant.equals(request.tenantId()))
            .findFirst()
            .orElseThrow();
    assertThat(tenantRequest.audienceType()).isEqualTo(NotificationAudienceType.TENANT_ADMINS);
    assertThat(tenantRequest.dedupeKey())
        .isEqualTo(
            "drawresult.corrected:%s:%s:TENANT_ADMINS"
                .formatted(drawResultId.value(), tenant.value()));
    assertThat(tenantRequest.channels()).containsExactly(NotificationChannel.WEB);
    assertThat(tenantRequest.translations()).containsOnlyKeys("fr", "en", "ht");
    assertThat(tenantRequest.translations().get("fr").title()).isEqualTo("Résultat corrigé");
  }

  private static DrawResultAffectedTenant affected(
      TenantId tenantId, String channelCode, String channelLabel) {
    return new DrawResultAffectedTenant(
        tenantId,
        DrawId.of(UUID.randomUUID()),
        DrawChannelId.of(UUID.randomUUID()),
        channelCode,
        channelLabel,
        "FL_EVE");
  }
}
