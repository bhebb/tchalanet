package com.tchalanet.server.platform.audit.internal.service;

import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.platform.audit.api.AuditApi;
import com.tchalanet.server.platform.audit.api.model.AuditEventView;
import com.tchalanet.server.platform.audit.api.model.PurgeOldAuditEventsResult;
import com.tchalanet.server.platform.audit.api.model.request.ListAuditEventsRequest;
import com.tchalanet.server.platform.audit.api.model.request.LogAuditEventRequest;
import com.tchalanet.server.platform.audit.api.model.request.PurgeOldAuditEventsRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DefaultAuditApi implements AuditApi {

  private final AuditService auditService;

  @Override
  public void logAuditEvent(LogAuditEventRequest request) {
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
