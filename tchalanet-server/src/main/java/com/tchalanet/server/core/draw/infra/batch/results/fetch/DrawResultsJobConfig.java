package com.tchalanet.server.core.draw.infra.batch.results.fetch;

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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.UUID;

@Configuration
@EnableBatchProcessing
@RequiredArgsConstructor
public class DrawResultsJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager batchTxManager;
    private final ItemReader<UUID> fetchableDrawIdsReader;
    private final ItemProcessor<UUID, ApplyResultRow> fetchExternalResultProcessor;
    private final ItemWriter<ApplyResultRow> applyResultWriter;

    @Value("${app.batch.fetch.chunk-size:10}")
    private int chunkSize;

    @Bean
    public Job fetchDrawResultsJob() {
        return new JobBuilder("fetch_draw_results", jobRepository)
            .start(fetchStep())
            .build();
    }

    @Bean
    public Step fetchStep() {
        return new StepBuilder("fetchDrawResultsStep", jobRepository)
            .<UUID, ApplyResultRow>chunk(chunkSize)
            .transactionManager(batchTxManager)
            .reader(fetchableDrawIdsReader)
            .processor(fetchExternalResultProcessor)
            .writer(applyResultWriter)
            .build();
    }
}
