package com.tchalanet.server.platform.audit.internal.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tchalanet.server.common.context.TchContextResolver;
import com.tchalanet.server.platform.audit.api.model.AuditAction;
import com.tchalanet.server.platform.audit.api.model.AuditEntityType;
import com.tchalanet.server.platform.audit.api.model.request.LogAuditEventRequest;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DefaultAuditApiTest {

  private final AuditService auditService = mock(AuditService.class);
  private final TchContextResolver contextResolver = mock(TchContextResolver.class);
  private final DefaultAuditApi auditApi = new DefaultAuditApi(auditService, contextResolver);

  @Test
  void logAuditEventDoesNotPropagateAuditInfrastructureFailure() {
    var request =
        new LogAuditEventRequest(
            AuditEntityType.USER,
            "user-1",
            AuditAction.APP_USER_BOOTSTRAP_CREATED,
            Map.of("reasonCode", "test"));
    doThrow(new IllegalStateException("audit commit failed"))
        .when(auditService)
        .logAuditEvent(request);

    assertThatCode(() -> auditApi.logAuditEvent(request)).doesNotThrowAnyException();

    verify(auditService).logAuditEvent(request);
  }

  @Test
  void tenantScopedAuditFailureInsideTemporaryContextDoesNotPropagate() {
    var request =
        new LogAuditEventRequest(
            AuditEntityType.USER,
            "user-1",
            AuditAction.APP_USER_BOOTSTRAP_CREATED,
            Map.of("reasonCode", "test"),
            UUID.randomUUID());
    when(contextResolver.currentOrNull()).thenReturn(null);
    doThrow(new IllegalStateException("rls rejected audit insert"))
        .when(auditService)
        .logAuditEvent(any(LogAuditEventRequest.class));

    assertThatCode(() -> auditApi.logAuditEvent(request)).doesNotThrowAnyException();

    verify(auditService).logAuditEvent(request);
  }
}
