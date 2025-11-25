package com.tchalanet.server.common.batch;

import com.tchalanet.server.draw.domain.usecase.SettleDrawsUseCase;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

@Component
public class SettleDrawsTasklet implements Tasklet {

  private final SettleDrawsUseCase useCase;

  public SettleDrawsTasklet(SettleDrawsUseCase useCase) {
    this.useCase = useCase;
  }

  @Override
  public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext)
      throws Exception {
    useCase.execute();
    return RepeatStatus.FINISHED;
  }
}
