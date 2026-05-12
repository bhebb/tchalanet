package com.tchalanet.server.platform.audit.internal.service;

import com.tchalanet.server.platform.audit.api.model.PurgeOldAuditEventsCommand;
import org.springframework.stereotype.Component;

@Component
public class PurgeOldAuditEventsTasklet {

  private final PurgeOldAuditEventsCommandHandler useCase;

  public PurgeOldAuditEventsTasklet(PurgeOldAuditEventsCommandHandler useCase) {
    this.useCase = useCase;
  }

  // Temporary: simple method to trigger purge without depending on Spring Batch types.
  public void executePurge() throws Exception {
    useCase.handle(new PurgeOldAuditEventsCommand());
  }
}
