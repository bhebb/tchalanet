package com.tchalanet.server.core.draw.infra.batch.config;

import com.tchalanet.server.core.draw.application.command.handler.SettleDrawsCommandHandler;
import com.tchalanet.server.core.draw.application.command.model.SettleDrawCommand;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.builder.StepBuilderException;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.Instant;
import java.util.UUID;

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
    @StepScope
    public ItemProcessor<UUID, SettleDrawCommand> settleDrawProcessor(
        @Value("#{jobParameters}") JobParameters jobParameters) {
        JobCtx ctx = JobCtx.from(jobParameters);
        return u -> new SettleDrawCommand(ctx.tenantId(), u);
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

    private record JobCtx(UUID tenantId, boolean dryRun, boolean force, String source, String provider,
                          String channelCode, Long daysBack, Long maxDraws, Instant ts) {
        static JobCtx from(JobParameters jp) {
            String tenantIdStr = jp.getString("tenant_id");
            if (tenantIdStr == null || tenantIdStr.isBlank()) {
                throw new StepBuilderException(new IllegalArgumentException("tenant_id required"));
            }
            UUID tenantId = UUID.fromString(tenantIdStr);
            boolean dryRun = "true".equalsIgnoreCase(jp.getString("dry_run"));
            boolean force = "true".equalsIgnoreCase(jp.getString("force"));
            String source = jp.getString("source");
            String provider = jp.getString("provider");
            String channelCode = jp.getString("channel_code");
            Long daysBack = jp.getLong("days_back");
            Long maxDraws = jp.getLong("max_draws");
            Instant ts = jp.getLong("ts") != null ? Instant.ofEpochMilli(jp.getLong("ts")) : Instant.now();
            return new JobCtx(tenantId, dryRun, force, source, provider, channelCode, daysBack, maxDraws, ts);
        }
    }
}
