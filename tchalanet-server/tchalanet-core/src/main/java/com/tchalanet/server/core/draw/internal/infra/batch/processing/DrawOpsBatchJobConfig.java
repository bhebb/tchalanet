package com.tchalanet.server.core.draw.internal.infra.batch.processing;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.job.params.JobParamKeys;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.draw.api.command.ApplyExternalResultsWindowCommand;
import com.tchalanet.server.core.draw.api.command.CloseDueDrawsCommand;
import com.tchalanet.server.core.draw.api.command.GenerateDrawsForRangeCommand;
import com.tchalanet.server.core.draw.api.command.OpenTodayDrawsCommand;
import com.tchalanet.server.core.drawresult.api.command.FetchExternalResultsWindowCommand;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.listener.JobExecutionListener;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
public class DrawOpsBatchJobConfig {

  private static final String FROM = "from";
  private static final String TO = "to";
  private static final String DATE = "date";
  private static final String DAYS_AHEAD = "days_ahead";
  private static final String DAYS_BACK = "days_back";
  private static final String SLOT_KEY = "slot_key";
  private static final String SLOT_KEYS = "slot_keys";
  private static final String MAX_DRAWS = "max_draws";
  private static final String MAX_SLOTS = "max_slots";
  private static final String REASON = "reason";

  private final JobRepository jobRepository;
  private final PlatformTransactionManager batchTxManager;
  private final JobExecutionListener listener;
  private final CommandBus commandBus;
  private final Clock clock;

  @Bean(name = "generateDrawsJob")
  public Job generateDrawsJob(@Qualifier("generateDrawsStep") Step step) {
    return job("generate_draws", step);
  }

  @Bean(name = "openDrawsJob")
  public Job openDrawsJob(@Qualifier("openDrawsStep") Step step) {
    return job("open_draws", step);
  }

  @Bean(name = "closeDrawsJob")
  public Job closeDrawsJob(@Qualifier("closeDrawsStep") Step step) {
    return job("close_draws", step);
  }

  @Bean(name = "fetchResultsJob")
  public Job fetchResultsJob(@Qualifier("fetchResultsStep") Step step) {
    return job("fetch_results", step);
  }

  @Bean(name = "applyResultsJob")
  public Job applyResultsJob(@Qualifier("applyResultsStep") Step step) {
    return job("apply_results", step);
  }

  @Bean
  public Step generateDrawsStep(@Qualifier("generateDrawsTasklet") Tasklet tasklet) {
    return taskletStep("generateDrawsStep", tasklet);
  }

  @Bean
  public Step openDrawsStep(@Qualifier("openDrawsTasklet") Tasklet tasklet) {
    return taskletStep("openDrawsStep", tasklet);
  }

  @Bean
  public Step closeDrawsStep(@Qualifier("closeDrawsTasklet") Tasklet tasklet) {
    return taskletStep("closeDrawsStep", tasklet);
  }

  @Bean
  public Step fetchResultsStep(@Qualifier("fetchResultsTasklet") Tasklet tasklet) {
    return taskletStep("fetchResultsStep", tasklet);
  }

  @Bean
  public Step applyResultsStep(@Qualifier("applyResultsTasklet") Tasklet tasklet) {
    return taskletStep("applyResultsStep", tasklet);
  }

  @Bean
  @org.springframework.batch.core.configuration.annotation.StepScope
  public Tasklet generateDrawsTasklet(
      @Value("#{jobParameters['tenant_id']}") String tenantId,
      @Value("#{jobParameters['from']}") String from,
      @Value("#{jobParameters['to']}") String to,
      @Value("#{jobParameters['days_ahead']}") String daysAhead,
      @Value("#{jobParameters['dry_run']}") String dryRun,
      @Value("#{jobParameters['force']}") String force,
      @Value("#{jobParameters['reason']}") String reason) {
    return (contribution, chunkContext) -> {
      LocalDate start = parseDate(from, todayUtc());
      int ahead = parseInt(daysAhead, 7);
      LocalDate end = parseDate(to, start.plusDays(Math.max(1, ahead) - 1L));
      commandBus.execute(new GenerateDrawsForRangeCommand(
          parseTenantId(tenantId),
          start,
          end,
          parseBoolean(dryRun),
          parseBoolean(force),
          trimToNull(reason)));
      return RepeatStatus.FINISHED;
    };
  }

  @Bean
  @org.springframework.batch.core.configuration.annotation.StepScope
  public Tasklet openDrawsTasklet(
      @Value("#{jobParameters['date']}") String date,
      @Value("#{jobParameters['max_items']}") String maxItems,
      @Value("#{jobParameters['dry_run']}") String dryRun) {
    return (contribution, chunkContext) -> {
      commandBus.execute(new OpenTodayDrawsCommand(
          Instant.now(clock),
          parseDateOrNull(date),
          parseInt(maxItems, 100),
          parseBoolean(dryRun)));
      return RepeatStatus.FINISHED;
    };
  }

  @Bean
  @org.springframework.batch.core.configuration.annotation.StepScope
  public Tasklet closeDrawsTasklet(
      @Value("#{jobParameters['max_items']}") String maxItems,
      @Value("#{jobParameters['dry_run']}") String dryRun) {
    return (contribution, chunkContext) -> {
      commandBus.execute(new CloseDueDrawsCommand(
          Instant.now(clock),
          parseInt(maxItems, 100),
          parseBoolean(dryRun)));
      return RepeatStatus.FINISHED;
    };
  }

  @Bean
  @org.springframework.batch.core.configuration.annotation.StepScope
  public Tasklet fetchResultsTasklet(
      @Value("#{jobParameters['tenant_id']}") String tenantId,
      @Value("#{jobParameters['date']}") String date,
      @Value("#{jobParameters['days_back']}") String daysBack,
      @Value("#{jobParameters['slot_key']}") String slotKey,
      @Value("#{jobParameters['slot_keys']}") String slotKeys,
      @Value("#{jobParameters['max_slots']}") String maxSlots,
      @Value("#{jobParameters['dry_run']}") String dryRun,
      @Value("#{jobParameters['force']}") String force,
      @Value("#{jobParameters['reason']}") String reason) {
    return (contribution, chunkContext) -> {
      commandBus.execute(new FetchExternalResultsWindowCommand(
          parseOptionalTenantId(tenantId),
          parseDate(date, todayUtc()),
          parseInt(daysBack, 0),
          parseSlotKeys(slotKey, slotKeys),
          parseBoolean(force),
          parseBoolean(dryRun),
          parseInt(maxSlots, 1),
          trimToNull(reason),
          false));
      return RepeatStatus.FINISHED;
    };
  }

  @Bean
  @org.springframework.batch.core.configuration.annotation.StepScope
  public Tasklet applyResultsTasklet(
      @Value("#{jobParameters['tenant_id']}") String tenantId,
      @Value("#{jobParameters['date']}") String date,
      @Value("#{jobParameters['days_back']}") String daysBack,
      @Value("#{jobParameters['slot_key']}") String slotKey,
      @Value("#{jobParameters['slot_keys']}") String slotKeys,
      @Value("#{jobParameters['max_slots']}") String maxSlots,
      @Value("#{jobParameters['dry_run']}") String dryRun,
      @Value("#{jobParameters['force']}") String force,
      @Value("#{jobParameters['reason']}") String reason) {
    return (contribution, chunkContext) -> {
      commandBus.execute(new ApplyExternalResultsWindowCommand(
          parseTenantId(tenantId),
          parseDate(date, todayUtc()),
          parseInt(daysBack, 0),
          parseSlotKeys(slotKey, slotKeys),
          parseBoolean(force),
          parseBoolean(dryRun),
          parseInt(maxSlots, 1),
          trimToNull(reason)));
      return RepeatStatus.FINISHED;
    };
  }

  private Job job(String name, Step step) {
    return new JobBuilder(name, jobRepository)
        .listener(listener)
        .start(step)
        .build();
  }

  private Step taskletStep(String name, Tasklet tasklet) {
    return new StepBuilder(name, jobRepository)
        .tasklet(tasklet)
        .transactionManager(batchTxManager)
        .build();
  }

  private LocalDate todayUtc() {
    return LocalDate.now(clock.withZone(ZoneOffset.UTC));
  }

  private static TenantId parseTenantId(String value) {
    String normalized = trimToNull(value);
    if (normalized == null) {
      throw new IllegalArgumentException(JobParamKeys.TENANT_ID + " is required");
    }
    return TenantId.parse(normalized);
  }

  private static TenantId parseOptionalTenantId(String value) {
    String normalized = trimToNull(value);
    return normalized != null ? TenantId.parse(normalized) : null;
  }

  private static LocalDate parseDate(String value, LocalDate fallback) {
    String normalized = trimToNull(value);
    return normalized != null ? LocalDate.parse(normalized) : fallback;
  }

  private static LocalDate parseDateOrNull(String value) {
    String normalized = trimToNull(value);
    return normalized != null ? LocalDate.parse(normalized) : null;
  }

  private static int parseInt(String value, int fallback) {
    String normalized = trimToNull(value);
    return normalized != null ? Integer.parseInt(normalized) : fallback;
  }

  private static boolean parseBoolean(String value) {
    return Boolean.parseBoolean(trimToNull(value));
  }

  private static List<String> parseSlotKeys(String slotKey, String slotKeys) {
    String raw = trimToNull(slotKeys);
    if (raw == null) {
      raw = trimToNull(slotKey);
    }
    if (raw == null) {
      return List.of();
    }
    return Arrays.stream(raw.split(","))
        .map(String::trim)
        .filter(value -> !value.isBlank())
        .toList();
  }

  private static String trimToNull(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }
}
