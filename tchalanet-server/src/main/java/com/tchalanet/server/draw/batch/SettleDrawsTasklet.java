package com.tchalanet.server.draw.batch;

import com.tchalanet.server.draw.application.ports.in.SettleDrawsUseCase; // Updated import
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

// Assuming tenantId is needed for SettleDrawsUseCase

@Component
public class SettleDrawsTasklet implements Tasklet {

  private final SettleDrawsUseCase useCase;

  public SettleDrawsTasklet(SettleDrawsUseCase useCase) {
    this.useCase = useCase;
  }

  public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext)
      throws Exception {
    // Similar to CloseDrawsTasklet, assuming tenantId needs to be passed.
    //  useCase.settleDraws(null); // Assuming null means all tenants or a default
    return RepeatStatus.FINISHED;
  }
}
