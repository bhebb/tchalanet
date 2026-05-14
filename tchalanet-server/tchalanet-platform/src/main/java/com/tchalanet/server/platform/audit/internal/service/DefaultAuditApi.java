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

  private final AuditLoggingCommandHandler auditLogging;
  private final ListAuditEventsQueryHandler listAuditEvents;
  private final PurgeOldAuditEventsCommandHandler purgeOldAuditEvents;

  @Override
  public void logAuditEvent(LogAuditEventRequest request) {
    auditLogging.handle(request);
  }

  @Override
  public TchPage<AuditEventView> listAuditEvents(ListAuditEventsRequest request) {
    return listAuditEvents.handle(request);
  }

  @Override
  public PurgeOldAuditEventsResult purgeOldAuditEvents(PurgeOldAuditEventsRequest request) {
    return purgeOldAuditEvents.handle(request);
  }
}
