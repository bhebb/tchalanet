package com.tchalanet.server.core.draw.infra.batch.results.fetch;

import com.tchalanet.server.common.batch.BatchTchContextJobListener;
import com.tchalanet.server.common.types.id.DrawId;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class DrawResultsJobConfig {

  private final JobRepository jobRepository;
  private final PlatformTransactionManager batchTxManager;
  private final ItemReader<DrawId> fetchableDrawIdsReader;
  private final ItemProcessor<DrawId, ApplyResultRow> fetchExternalResultProcessor;
  private final ItemWriter<ApplyResultRow> applyResultWriter;
  private final BatchTchContextJobListener listener;

  @Value("${app.batch.fetch.chunk-size:10}")
  private int chunkSize;

  @Bean
  public Job fetchDrawResultsJob() {
    return new JobBuilder("fetch_draw_results", jobRepository)
        .listener(listener)
        .start(fetchStep())
        .build();
  }

  @Bean
  public Step fetchStep() {
    return new StepBuilder("fetchDrawResultsStep", jobRepository)
        .<DrawId, ApplyResultRow>chunk(chunkSize)
        .transactionManager(batchTxManager)
        .reader(fetchableDrawIdsReader)
        .processor(fetchExternalResultProcessor)
        .writer(applyResultWriter)
        .build();
  }
}
