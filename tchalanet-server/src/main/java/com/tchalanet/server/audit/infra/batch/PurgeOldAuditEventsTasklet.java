package com.tchalanet.server.audit.infra.batch;

import com.tchalanet.server.audit.application.PurgeOldAuditEventsUseCase;
import com.tchalanet.server.audit.application.command.model.PurgeOldAuditEventsCommand;
import org.springframework.stereotype.Component;

@Component
public class PurgeOldAuditEventsTasklet {

  private final PurgeOldAuditEventsUseCase useCase;

  public PurgeOldAuditEventsTasklet(PurgeOldAuditEventsUseCase useCase) {
    this.useCase = useCase;
  }

  // Temporary: simple method to trigger purge without depending on Spring Batch types.
  public void executePurge() throws Exception {
    useCase.handle(new PurgeOldAuditEventsCommand());
  }
}
