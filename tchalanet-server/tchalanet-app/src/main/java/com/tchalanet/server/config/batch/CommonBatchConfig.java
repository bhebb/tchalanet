package com.tchalanet.server.config.batch;

import javax.sql.DataSource;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.EnableJdbcJobRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableBatchProcessing(taskExecutorRef = "batchTaskExecutor")
@EnableJdbcJobRepository(
    dataSourceRef = "batchDataSource",
    transactionManagerRef = "batchTxManager",
    tablePrefix = "batch.BATCH_")
@EnableScheduling
public class CommonBatchConfig {

  @Bean
  public JdbcTransactionManager batchTxManager(@Qualifier("batchDataSource") DataSource ds) {
    return new JdbcTransactionManager(ds);
  }

  @Bean
  public TaskExecutor batchTaskExecutor() {
    ThreadPoolTaskExecutor exec = new ThreadPoolTaskExecutor();
    exec.setCorePoolSize(4);
    exec.setMaxPoolSize(8);
    exec.setThreadNamePrefix("batch-");
    exec.initialize();
    return exec;
  }
}
