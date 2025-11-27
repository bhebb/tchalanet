package com.tchalanet.server.dev.placeholders.batch.core.step.tasklet;

import com.tchalanet.server.dev.placeholders.batch.core.StepContribution;
import com.tchalanet.server.dev.placeholders.batch.core.repeat.RepeatStatus;
import com.tchalanet.server.dev.placeholders.batch.core.scope.context.ChunkContext;

public interface Tasklet {
  RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception;
}
