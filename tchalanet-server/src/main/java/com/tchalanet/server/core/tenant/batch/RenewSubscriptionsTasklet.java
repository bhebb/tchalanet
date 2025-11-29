package com.tchalanet.server.core.tenant.batch;

import com.tchalanet.server.core.tenant.domain.usecase.subscription.RenewSubscriptionsUseCase; // Assuming this is the correct path
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RenewSubscriptionsTasklet implements Tasklet {

  private final RenewSubscriptionsUseCase useCase;

  @Override
  public @Nullable RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext)
      throws Exception {
    return null;
  }
}
