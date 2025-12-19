package com.tchalanet.server.core.draw.infra.batch.config;

import com.tchalanet.server.core.draw.application.port.out.DrawResultWriterPort;
import com.tchalanet.server.core.draw.application.port.out.ExternalDrawResultPort;
import com.tchalanet.server.core.draw.domain.model.DrawResult;
import com.tchalanet.server.core.draw.domain.model.DrawSource;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.JobParameters;
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
@Slf4j
public class FetchDrawResultsJobConfig {
  public static final String JOB_NAME = "fetch_draw_results";

  @Bean
  public Job fetchDrawResultsJob(JobRepository jobRepository, Step fetchDrawResultsStep) {
    return new JobBuilder(JOB_NAME, jobRepository).start(fetchDrawResultsStep).build();
  }

  @Bean
  public Step fetchDrawResultsStep(
      JobRepository jobRepository,
      PlatformTransactionManager batchTxManager,
      ItemReader<UUID> fetchableDrawIdsReader,
      ItemProcessor<UUID, UUID> fetchDrawResultsProcessor,
      ItemWriter<UUID> fetchDrawResultsWriter,
      @Value("${app.batch.fetch-results.chunk-size:5000}") int chunkSize) {

    return new StepBuilder(jobRepository)
        .<UUID, UUID>chunk(chunkSize)
        .transactionManager(batchTxManager)
        .reader(fetchableDrawIdsReader)
        .processor(fetchDrawResultsProcessor)
        .writer(fetchDrawResultsWriter)
        .build();
  }

  @Bean
  @StepScope
  public ItemProcessor<UUID, UUID> fetchDrawResultsProcessor() {
    // noop (on garde le pipeline existant)
    return drawId -> drawId;
  }

  @Bean
  @StepScope
  public ItemWriter<UUID> fetchDrawResultsWriter(
      ExternalDrawResultPort externalDrawResultPort,
      DrawResultWriterPort drawResultWriterPort,
      Clock clock,
      @Value("#{jobParameters}") JobParameters jp) {

    AtomicReference<ExternalDrawResultPort.ExternalDrawResult> cachedExternal = new AtomicReference<>();

    return chunk -> {
      JobCtx ctx = JobCtx.from(jp, clock);

      ExternalDrawResultPort.ExternalDrawResult external = cachedExternal.get();
      if (external == null) {
        external =
            externalDrawResultPort.fetchExternalResult(
                new ExternalDrawResultPort.DrawExternalQuery(
                    ctx.channelCode, ctx.drawDateLocal, ctx.now, ctx.force));
        cachedExternal.set(external);
      }

      log.info(
          "fetch_draw_results(slot-writer): tenantId={} channelCode={} drawDateLocal={} countDraws={} found={} status={} dryRun={}",
          ctx.tenantId,
          ctx.channelCode,
          ctx.drawDateLocal,
          chunk.getItems().size(),
          external.found(),
          external.status(),
          ctx.dryRun);

      if (!external.found() || ctx.dryRun) {
        return;
      }

      DrawResult result =
          new DrawResult(
              DrawSource.EXTERNAL,
              external.numbers(),
              external.numbersExtra(),
              external.occurredAt() != null ? external.occurredAt() : ctx.now,
              external.rawPayload() == null ? "" : external.rawPayload().toString(),
              false,
              null);

      for (UUID drawId : chunk.getItems()) {
        drawResultWriterPort.save(ctx.tenantId, drawId, result);
      }
    };
  }

  private record JobCtx(UUID tenantId, String channelCode, LocalDate drawDateLocal, Instant now, boolean force, boolean dryRun) {
    private static JobCtx from(JobParameters jp, Clock clock) {
      String tenantIdStr = jp.getString("tenant_id");
      String channelCode = jp.getString("channel_code");
      String drawDateStr = jp.getString("draw_date");
      boolean force = "true".equalsIgnoreCase(jp.getString("force"));
      boolean dryRun = "true".equalsIgnoreCase(jp.getString("dry_run"));

      if (tenantIdStr == null || tenantIdStr.isBlank()) {
        throw new IllegalArgumentException("tenant_id required");
      }
      if (channelCode == null || channelCode.isBlank()) {
        throw new IllegalArgumentException("channel_code required");
      }

      UUID tenantId = UUID.fromString(tenantIdStr);
      Instant now = Instant.now(clock);

      LocalDate drawDateLocal =
          (drawDateStr == null || drawDateStr.isBlank())
              ? ZonedDateTime.ofInstant(now, ZoneId.of("UTC")).toLocalDate()
              : LocalDate.parse(drawDateStr);

      return new JobCtx(tenantId, channelCode, drawDateLocal, now, force, dryRun);
    }
  }
}
