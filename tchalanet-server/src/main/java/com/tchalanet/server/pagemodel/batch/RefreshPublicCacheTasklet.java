package com.tchalanet.server.pagemodel.batch;

import com.tchalanet.server.draw.application.ports.in.RefreshPublicDrawsCacheUseCase;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

@Component
public class RefreshPublicCacheTasklet implements Tasklet {

  private final RefreshPublicDrawsCacheUseCase useCase;

  public RefreshPublicCacheTasklet(RefreshPublicDrawsCacheUseCase useCase) {
    this.useCase = useCase;
  }

  public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext)
      throws Exception {
    // Assuming refreshAllTenants() processes all tenants or a default.
    // If it needs a tenantId, it should be passed from batch job parameters.
    useCase.refreshCache(null);
    return RepeatStatus.FINISHED;
  }
}
