package com.tchalanet.server.core.draw.infra.batch.results.settle;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.UUID;

@Configuration
@EnableBatchProcessing
@RequiredArgsConstructor
public class DrawSettleJobConfig {

  private final JobRepository jobRepository;
  private final PlatformTransactionManager batchTxManager;
  private final ItemReader<UUID> settleableDrawIdsReader;
  private final ItemProcessor<UUID, UUID> settleProcessor;
  private final ItemWriter<UUID> settleWriter;

  @Bean
  public Job settleDrawsJob() {
    return new JobBuilder("settle_draws", jobRepository)
        .start(settleStep())
        .build();
  }

  @Bean
  public Step settleStep() {
    return new StepBuilder("settleDrawsStep", jobRepository)
        .<UUID, UUID>chunk(10)
        .transactionManager(batchTxManager)
        .reader(settleableDrawIdsReader)
        .processor(settleProcessor)
        .writer(settleWriter)
        .build();
  }
}
