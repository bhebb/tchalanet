package com.tchalanet.server.draw.batch;

import com.tchalanet.server.draw.application.ports.in.CloseDueDrawsUseCase; // Updated import
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

// Assuming tenantId is needed for CloseDueDrawsUseCase

@Component
public class CloseDrawsTasklet implements Tasklet {

  private final CloseDueDrawsUseCase useCase;

  public CloseDrawsTasklet(CloseDueDrawsUseCase useCase) {
    this.useCase = useCase;
  }

  public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext)
      throws Exception {
    // In a multi-tenant context, you might need to iterate over tenants
    // For now, assuming it processes for a default tenant or all tenants if useCase supports it.
    // The CloseDueDrawsUseCase already takes a tenantId.
    // This would need to be passed from the batch job parameters or a global context.
    // For demonstration, let's assume a placeholder tenantId or null for all.
    //    useCase.closeDueDraws(null); // Assuming null means all tenants or a default
    return RepeatStatus.FINISHED;
  }
}
