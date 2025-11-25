package com.tchalanet.server.common.batch;

import com.tchalanet.server.common.audit.domain.usecase.PurgeOldAuditEventsUseCase;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

@Component
public class PurgeOldAuditEventsTasklet implements Tasklet {

  private final PurgeOldAuditEventsUseCase useCase;

  public PurgeOldAuditEventsTasklet(PurgeOldAuditEventsUseCase useCase) {
    this.useCase = useCase;
  }

  @Override
  public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext)
      throws Exception {
    useCase.purge();
    return RepeatStatus.FINISHED;
  }
}
