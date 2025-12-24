package com.tchalanet.server.core.billing.infra.batch;

import com.tchalanet.server.core.billing.application.command.handler.RenewSubscriptionsCommandHandler;
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

  private final RenewSubscriptionsCommandHandler renewSubscriptionsCommandHandler;

  @Override
  public @Nullable RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext)
      throws Exception {
    return null;
  }
}
