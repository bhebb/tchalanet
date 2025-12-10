package com.tchalanet.server.core.draw.infra.batch.config;

import com.tchalanet.server.core.draw.application.command.handler.FetchAndApplyExternalResultCommandHandler;
import com.tchalanet.server.core.draw.application.command.model.FetchAndApplyExternalResultCommand;
import com.tchalanet.server.core.draw.domain.model.DrawSource;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class FetchDrawResultsJobConfig {
  public static final String JOB_NAME = "fetch_draw_results";

    private final int chunkSize;

    public FetchDrawResultsJobConfig(@Value("${app.batch.settle.chunk-size:50}") int chunkSize) {
        this.chunkSize = chunkSize;
    }

    @Bean
  public Job fetchDrawResultsJob(JobRepository jobRepository, Step fetchDrawResultsStep) {
    return new JobBuilder(JOB_NAME, jobRepository).start(fetchDrawResultsStep).build();
  }

  @Bean
  public Step fetchDrawResultsStep(
      JobRepository jobRepository,
      PlatformTransactionManager batchTxManager,
      ItemReader<UUID> fetchableDrawIdsReader,
      ItemProcessor<UUID, FetchAndApplyExternalResultCommand> fetchDrawResultsProcessor,
      ItemWriter<FetchAndApplyExternalResultCommand> fetchDrawResultsWriter) {

    return new StepBuilder(jobRepository)
        .<UUID, FetchAndApplyExternalResultCommand>chunk(chunkSize)
        .transactionManager(batchTxManager)
        .reader(fetchableDrawIdsReader)
        .processor(fetchDrawResultsProcessor)
        .writer(fetchDrawResultsWriter)
        .build();
  }

  @Bean
  public ItemProcessor<UUID, FetchAndApplyExternalResultCommand> fetchDrawResultsProcessor(Clock clock) {
    return drawId -> {
      var now = Instant.now(clock);
      // Tu pourras affiner la fenêtre ici (near real-time vs backfill)
      return new FetchAndApplyExternalResultCommand(drawId, now);
    };
  }

  @Bean
  public ItemWriter<FetchAndApplyExternalResultCommand> fetchDrawResultsWriter(FetchAndApplyExternalResultCommandHandler fetchDrawResultsCommandHandler) {
    return items -> {
      for (FetchAndApplyExternalResultCommand cmd : items) {
        fetchDrawResultsCommandHandler.handle(cmd);
      }
    };
  }
}
