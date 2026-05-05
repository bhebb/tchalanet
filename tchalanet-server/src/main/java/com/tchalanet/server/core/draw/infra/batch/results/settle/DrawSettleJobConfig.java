package com.tchalanet.server.core.draw.infra.batch.results.settle;

import com.tchalanet.server.common.batch.context.BatchJobExecutionListener;
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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
public class DrawSettleJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager batchTxManager;

    private final ItemReader<DrawId> settleableDrawIdsReader;
    private final ItemProcessor<DrawId, DrawId> settleProcessor;
    private final ItemWriter<DrawId> settleWriter;

    private final BatchJobExecutionListener listener;

    @Bean(name = "settleDrawsJob") // must match TchBatchJobRegistry springJobBeanName
    public Job settleDrawsJob() {
        return new JobBuilder("settle_draws", jobRepository)
            .listener(listener)
            .start(settleStep())
            .build();
    }

    @Bean
    public Step settleStep() {
        return new StepBuilder("settleDrawsStep", jobRepository)
            .<DrawId, DrawId>chunk(10)          // ✅ SB6: use chunk(int)
            .transactionManager(batchTxManager) // ✅ SB6: set tx manager here
            .reader(settleableDrawIdsReader)
            .processor(settleProcessor)
            .writer(settleWriter)
            .build();
    }
}
