package com.tchalanet.server.common.batch;

import com.tchalanet.server.draw.domain.usecase.RefreshPublicDrawsCacheUseCase;
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

  @Override
  public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext)
      throws Exception {
    useCase.refreshAllTenants();
    return RepeatStatus.FINISHED;
  }
}
