package com.tchalanet.server.features.pagemodel.batch;

import com.tchalanet.server.core.draw.application.port.in.command.RefreshPublicDrawsCacheCommandHandler;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RefreshPublicCacheTasklet implements Tasklet {

  private final RefreshPublicDrawsCacheCommandHandler useCase;

  @Override
  public @Nullable RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext)
      throws Exception {
    return null;
  }
}
