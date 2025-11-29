package com.tchalanet.server.draw.infra.batch.config;

import com.tchalanet.server.draw.infra.batch.CloseDrawsTasklet;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class CloseDueDrawsJobConfig {

  public static final String JOB_NAME = "close_due_draws";

  @Bean
  public Job closeDueDrawsJob(JobRepository jobRepository, Step closeDueDrawsStep) {
    return new JobBuilder(JOB_NAME, jobRepository).start(closeDueDrawsStep).build();
  }

  @Bean
  public Step closeDueDrawsStep(
      JobRepository jobRepository,
      PlatformTransactionManager batchTxManager,
      CloseDrawsTasklet closeDrawsTasklet) {

    // Step Tasklet en Spring Batch 6 :
    // StepBuilder(jobRepository).tasklet(tasklet, txManager)
    return new StepBuilder(jobRepository).tasklet(closeDrawsTasklet, batchTxManager).build();
  }
}
