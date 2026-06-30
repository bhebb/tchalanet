package com.tchalanet.server.platform.audit.internal.service;

import com.tchalanet.server.common.context.TchContextScope;
import com.tchalanet.server.common.context.TchContextResolver;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.platform.audit.api.AuditApi;
import com.tchalanet.server.platform.audit.api.model.AuditEventView;
import com.tchalanet.server.platform.audit.api.model.PurgeOldAuditEventsResult;
import com.tchalanet.server.platform.audit.api.model.request.ListAuditEventsRequest;
import com.tchalanet.server.platform.audit.api.model.request.LogAuditEventRequest;
import com.tchalanet.server.platform.audit.api.model.request.PurgeOldAuditEventsRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class DefaultAuditApi implements AuditApi {

  private final AuditService auditService;
  private final TchContextResolver contextResolver;

  @Override
  public void logAuditEvent(LogAuditEventRequest request) {
    try {
      logAuditEventInternal(request);
    } catch (RuntimeException ex) {
      log.warn(
          "Functional audit event was not persisted; continuing without failing business flow"
              + " (entityType={}, entityId={}, action={}, error={}: {})",
          request == null ? null : request.entityType(),
          request == null ? null : request.entityId(),
          request == null ? null : request.action(),
          ex.getClass().getSimpleName(),
          ex.getMessage());
      log.debug("Functional audit persistence failure stacktrace", ex);
    }
  }

  private void logAuditEventInternal(LogAuditEventRequest request) {
    if (request != null && request.tenantId() != null) {
      var ctx = contextResolver.currentOrNull();
      if (ctx != null) {
        TchContextScope.runWithContext(
            ctx.withEffectiveTenantUuid(request.tenantId()),
            () -> auditService.logAuditEvent(request));
        return;
      }
      TchContextScope.runWithTemporaryTenant(request.tenantId(), "audit", () -> auditService.logAuditEvent(request));
      return;
    }
    auditService.logAuditEvent(request);
  }

  @Override
  public TchPage<AuditEventView> listAuditEvents(ListAuditEventsRequest request) {
    return auditService.listAuditEvents(request);
  }

  @Override
  public PurgeOldAuditEventsResult purgeOldAuditEvents(PurgeOldAuditEventsRequest request) {
    return auditService.purgeOldAuditEvents(request);
  }
}
