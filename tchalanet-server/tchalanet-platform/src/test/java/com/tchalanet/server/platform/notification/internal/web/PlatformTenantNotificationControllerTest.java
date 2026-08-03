package com.tchalanet.server.platform.notification.internal.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.tchalanet.server.common.context.TchContext;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.scope.ApiScope;
import com.tchalanet.server.common.security.TchRole;
import com.tchalanet.server.common.types.id.TenantId;
import java.time.ZoneId;
import java.util.Currency;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PlatformTenantNotificationControllerTest {

  private final NotificationAdminGate notificationAdminGate = mock(NotificationAdminGate.class);
  private final PlatformTenantNotificationController controller =
      new PlatformTenantNotificationController(notificationAdminGate);

  @Test
  void createsPlatformTenantAnnouncementInsideTheTargetTenantContext() {
    var targetTenantId = UUID.randomUUID();
    var context = platformContext();
    var request =
        new CreateNotificationBody(
            null, null, null, null, null, null, null, null, null, null, null, null, null, null,
            null, null, null, null);

    doAnswer(
            ignored -> {
              var activeContext = TchContext.currentOrThrow();
              assertThat(activeContext.effectiveTenantUuid()).isEqualTo(targetTenantId);
              assertThat(activeContext.tenantId()).isEqualTo(TenantId.of(targetTenantId));
              assertThat(activeContext.requestId()).isEqualTo(context.requestId());
              return null;
            })
        .when(notificationAdminGate)
        .createForTenantFromPlatform(request, TenantId.of(targetTenantId));

    controller.createAnnouncement(targetTenantId, request, context);

    verify(notificationAdminGate).createForTenantFromPlatform(request, TenantId.of(targetTenantId));
    assertThat(TchContext.currentOrNull()).isNull();
  }

  private static TchRequestContext platformContext() {
    return new TchRequestContext(
        null,
        null,
        null,
        null,
        UUID.randomUUID(),
        Set.of(TchRole.SUPER_ADMIN),
        Set.of(),
        Locale.CANADA_FRENCH,
        "request-test",
        "127.0.0.1",
        null,
        false,
        null,
        "active",
        ApiScope.PLATFORM,
        null,
        null,
        ZoneId.of("America/Port-au-Prince"),
        Currency.getInstance("HTG"),
        null,
        null,
        null,
        Set.of(TchRole.SUPER_ADMIN.name()),
        Set.of(),
        null);
  }
}
