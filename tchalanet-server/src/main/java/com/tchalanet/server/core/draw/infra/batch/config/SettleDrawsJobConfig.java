package com.tchalanet.server.core.draw.infra.batch.config;

import com.tchalanet.server.core.draw.application.command.model.SettleDrawCommand;
import com.tchalanet.server.core.draw.application.port.in.command.SettleDrawsCommandHandler;
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
public class SettleDrawsJobConfig {

  public static final String JOB_NAME = "settle_resulted_draws";

  @Value("${app.batch.settle.chunk-size:50}")
  private int chunkSize;

  @Bean
  public Job settleDrawsJob(JobRepository jobRepository, Step settleDrawsStep) {
    return new JobBuilder(JOB_NAME, jobRepository).start(settleDrawsStep).build();
  }

  @Bean
  public Step settleDrawsStep(
      JobRepository jobRepository,
      PlatformTransactionManager batchTxManager,
      ItemReader<UUID> settleableDrawIdsReader,
      ItemProcessor<UUID, SettleDrawCommand> settleDrawProcessor,
      ItemWriter<SettleDrawCommand> settleDrawWriter) {

    return new StepBuilder(jobRepository)
        .<UUID, SettleDrawCommand>chunk(chunkSize)
        .transactionManager(batchTxManager)
        .reader(settleableDrawIdsReader)
        .processor(settleDrawProcessor)
        .writer(settleDrawWriter)
        .build();
  }

  @Bean
  public ItemProcessor<UUID, SettleDrawCommand> settleDrawProcessor() {
    // todo only pass the draw id
    return u -> new SettleDrawCommand(null, u);
  }

  @Bean
  public ItemWriter<SettleDrawCommand> settleDrawWriter(
      SettleDrawsCommandHandler settleDrawsCommandHandler) {
    return items -> {
      for (SettleDrawCommand cmd : items) {
        settleDrawsCommandHandler.handle(cmd);
      }
    };
  }
}
