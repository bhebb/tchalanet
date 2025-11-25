package com.tchalanet.server.common.batch;

import com.tchalanet.server.tenant.domain.usecase.subscription.RenewSubscriptionsUseCase;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

@Component
public class RenewSubscriptionsTasklet implements Tasklet {

  private final RenewSubscriptionsUseCase useCase;

  public RenewSubscriptionsTasklet(RenewSubscriptionsUseCase useCase) {
    this.useCase = useCase;
  }

  @Override
  public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext)
      throws Exception {
    useCase.renewDueSubscriptions();
    return RepeatStatus.FINISHED;
  }
}
