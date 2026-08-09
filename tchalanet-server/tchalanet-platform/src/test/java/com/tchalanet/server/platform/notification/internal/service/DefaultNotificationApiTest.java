package com.tchalanet.server.platform.notification.internal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import com.tchalanet.server.common.context.TchContext;
import com.tchalanet.server.common.context.scope.ApiScope;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.platform.notification.api.model.NotificationAudienceType;
import com.tchalanet.server.platform.notification.api.model.NotificationCategory;
import com.tchalanet.server.platform.notification.api.model.NotificationKind;
import com.tchalanet.server.platform.notification.api.model.NotificationSeverity;
import com.tchalanet.server.platform.notification.api.model.request.CreateNotificationRequest;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class DefaultNotificationApiTest {

  @AfterEach
  void clearContext() {
    TchContext.clear();
  }

  @Test
  void createNotificationRejectsNullRequest() {
    var captured = new CapturedContext();
    var api = api(captured);

    assertThatNullPointerException()
        .isThrownBy(() -> api.createNotification(null))
        .withMessage("request");
  }

  @Test
  void createPlatformNotificationBindsPlatformSystemContextWhenMissing() {
    var captured = new CapturedContext();
    var api = api(captured);

    api.createNotification(request(null, NotificationAudienceType.PLATFORM_ADMINS));

    assertThat(captured.apiScope).isEqualTo(ApiScope.PLATFORM);
    assertThat(captured.superAdmin).isTrue();
    assertThat(captured.tenantUuid).isNull();
  }

  @Test
  void createTenantNotificationBindsTemporaryTenantContextWhenMissing() {
    var tenantId = TenantId.of(UUID.randomUUID());
    var captured = new CapturedContext();
    var api = api(captured);

    api.createNotification(request(tenantId, NotificationAudienceType.TENANT_ADMINS));

    assertThat(captured.apiScope).isEqualTo(ApiScope.TENANT);
    assertThat(captured.superAdmin).isFalse();
    assertThat(captured.tenantUuid).isEqualTo(tenantId.value());
  }

  private static DefaultNotificationApi api(CapturedContext captured) {
    var service = mock(NotificationService.class);
    doAnswer(
            invocation -> {
              var ctx = TchContext.currentOrNull();
              captured.apiScope = ctx == null ? null : ctx.apiScope();
              captured.superAdmin = ctx != null && ctx.isSuperAdmin();
              captured.tenantUuid = ctx == null ? null : ctx.tenantUuid();
              return null;
            })
        .when(service)
        .createNotification(org.mockito.ArgumentMatchers.any(CreateNotificationRequest.class));
    return new DefaultNotificationApi(service);
  }

  private static CreateNotificationRequest request(
      TenantId tenantId, NotificationAudienceType audienceType) {
    return new CreateNotificationRequest(
        tenantId,
        "TEST",
        UUID.randomUUID().toString(),
        UUID.randomUUID().toString(),
        audienceType,
        Set.of(),
        NotificationSeverity.INFO,
        NotificationKind.INFO,
        NotificationCategory.SYSTEM,
        null,
        null,
        "Title",
        "Message",
        null,
        null,
        null,
        null,
        null,
        Set.of());
  }

  private static final class CapturedContext {
    ApiScope apiScope;
    boolean superAdmin;
    UUID tenantUuid;
  }
}
