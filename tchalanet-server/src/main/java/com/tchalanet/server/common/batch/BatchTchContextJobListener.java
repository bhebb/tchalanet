package com.tchalanet.server.common.batch;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BatchTchContextJobListener implements JobExecutionListener {

  private final BatchTchContextBinder binder;

  @Override
  public void beforeJob(JobExecution jobExecution) {
    binder.bind(jobExecution.getJobParameters());
  }

  @Override
  public void afterJob(JobExecution jobExecution) {
    binder.clear();
  }
}
