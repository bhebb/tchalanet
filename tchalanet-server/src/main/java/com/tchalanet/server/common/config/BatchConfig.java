package com.tchalanet.server.common.config;

import com.tchalanet.server.common.batch.CloseDrawsTasklet;
import com.tchalanet.server.common.batch.PurgeOldAuditEventsTasklet;
import com.tchalanet.server.common.batch.RefreshPublicCacheTasklet;
import com.tchalanet.server.common.batch.RenewSubscriptionsTasklet;
import com.tchalanet.server.common.batch.SettleDrawsTasklet;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class BatchConfig {

  private final JobRepository jobRepository;

  public BatchConfig(JobRepository jobRepository) {
    this.jobRepository = jobRepository;
  }

  @Bean
  public Job closeDrawsJob(Step closeDrawsStep) {
    return new JobBuilder("closeDrawsJob", jobRepository)
        .incrementer(new RunIdIncrementer())
        .start(closeDrawsStep)
        .build();
  }

  @Bean
  public Job settleDrawsJob(Step settleDrawsStep) {
    return new JobBuilder("settleDrawsJob", jobRepository)
        .incrementer(new RunIdIncrementer())
        .start(settleDrawsStep)
        .build();
  }

  @Bean
  public Job refreshPublicCacheJob(Step refreshPublicCacheStep) {
    return new JobBuilder("refreshPublicCacheJob", jobRepository)
        .incrementer(new RunIdIncrementer())
        .start(refreshPublicCacheStep)
        .build();
  }

  @Bean
  public Job renewSubscriptionsJob(Step renewSubscriptionsStep) {
    return new JobBuilder("renewSubscriptionsJob", jobRepository)
        .incrementer(new RunIdIncrementer())
        .start(renewSubscriptionsStep)
        .build();
  }

  @Bean
  public Job purgeOldAuditEventsJob(Step purgeOldAuditEventsStep) {
    return new JobBuilder("purgeOldAuditEventsJob", jobRepository)
        .incrementer(new RunIdIncrementer())
        .start(purgeOldAuditEventsStep)
        .build();
  }

  @Bean
  public Step closeDrawsStep(CloseDrawsTasklet t, PlatformTransactionManager txManager) {
    return new StepBuilder("closeDrawsStep", jobRepository).tasklet(t, txManager).build();
  }

  @Bean
  public Step settleDrawsStep(SettleDrawsTasklet t, PlatformTransactionManager txManager) {
    return new StepBuilder("settleDrawsStep", jobRepository).tasklet(t, txManager).build();
  }

  @Bean
  public Step refreshPublicCacheStep(RefreshPublicCacheTasklet t, PlatformTransactionManager txManager) {
    return new StepBuilder("refreshPublicCacheStep", jobRepository).tasklet(t, txManager).build();
  }

  @Bean
  public Step renewSubscriptionsStep(RenewSubscriptionsTasklet t, PlatformTransactionManager txManager) {
    return new StepBuilder("renewSubscriptionsStep", jobRepository).tasklet(t, txManager).build();
  }

  @Bean
  public Step purgeOldAuditEventsStep(PurgeOldAuditEventsTasklet t, PlatformTransactionManager txManager) {
    return new StepBuilder("purgeOldAuditEventsStep", jobRepository).tasklet(t, txManager).build();
  }
}
