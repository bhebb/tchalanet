package com.tchalanet.server.platform.audit.internal.service;

import com.tchalanet.server.platform.audit.api.model.request.PurgeOldAuditEventsRequest;
import org.springframework.stereotype.Component;

@Component
public class PurgeOldAuditEventsTasklet {

  private final AuditService auditService;

  public PurgeOldAuditEventsTasklet(AuditService auditService) {
    this.auditService = auditService;
  }

  // Temporary: simple method to trigger purge without depending on Spring Batch types.
  public void executePurge() throws Exception {
    auditService.purgeOldAuditEvents(new PurgeOldAuditEventsRequest());
  }
}
