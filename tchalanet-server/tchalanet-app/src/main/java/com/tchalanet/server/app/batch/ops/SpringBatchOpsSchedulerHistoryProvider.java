package com.tchalanet.server.app.batch.ops;

import com.tchalanet.server.common.job.history.BatchJobHistoryService;
import com.tchalanet.server.features.pagemodel.dynamic.providers.platformadmin.OpsSchedulerHistoryProvider;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SpringBatchOpsSchedulerHistoryProvider implements OpsSchedulerHistoryProvider {

  private static final int RECENT_EXECUTION_LIMIT = 5;

  private final com.tchalanet.server.common.job.registry.TchJobRegistry jobRegistry;
  private final BatchJobHistoryService batchJobHistoryService;
  private final Duration staleAfter;

  public SpringBatchOpsSchedulerHistoryProvider(
      com.tchalanet.server.common.job.registry.TchJobRegistry jobRegistry,
      BatchJobHistoryService batchJobHistoryService,
      @Value("${tch.ops.scheduler.stale-after:PT1H}") Duration staleAfter) {
    this.jobRegistry = jobRegistry;
    this.batchJobHistoryService = batchJobHistoryService;
    this.staleAfter = staleAfter == null || staleAfter.isZero() || staleAfter.isNegative()
        ? Duration.ofHours(1)
        : staleAfter;
  }

  @Override
  public Snapshot snapshot() {
    List<ExecutionItem> recentExecutions = new ArrayList<>();
    long failedCount = 0L;
    long staleCount = 0L;
    long neverRunCount = 0L;
    Instant staleCutoff = Instant.now().minus(staleAfter);

    for (var registered : jobRegistry.list()) {
      var executions = batchJobHistoryService.listExecutions(registered.jobKey(), RECENT_EXECUTION_LIMIT);
      var latest = executions.stream()
          .max(Comparator.comparing(
              execution -> execution.startedAt() != null ? execution.startedAt() : Instant.EPOCH));

      if (latest.isEmpty()) {
        neverRunCount++;
        recentExecutions.add(new ExecutionItem(Instant.EPOCH, new Item(
            registered.jobKey().value(),
            registered.displayName(),
            registered.scope().name(),
            "NEVER_RUN",
            "WARNING",
            "/app/platform/ops/batch",
            null)));
        continue;
      }

      var execution = latest.get();
      String status = execution.status() != null ? execution.status() : "UNKNOWN";
      Instant startedAt = execution.startedAt();
      boolean stale = startedAt != null && startedAt.isBefore(staleCutoff);
      if ("FAILED".equals(status) || "ABANDONED".equals(status)) {
        failedCount++;
      }
      if (stale) {
        staleCount++;
      }
      recentExecutions.add(new ExecutionItem(
          startedAt,
          new Item(
              registered.jobKey().value(),
              registered.displayName(),
              registered.scope().name(),
              stale && "OK".equals(severity(status)) ? "STALE" : status,
              stale && "OK".equals(severity(status)) ? "WARNING" : severity(status),
              "/app/platform/ops/batch",
              execution.context())));
    }

    List<Item> items = recentExecutions.stream()
        .sorted(Comparator
            .comparing((ExecutionItem item) -> severityRank(item.item().severity()))
            .thenComparing(ExecutionItem::occurredAt, Comparator.nullsLast(Comparator.reverseOrder())))
        .limit(RECENT_EXECUTION_LIMIT)
        .map(ExecutionItem::item)
        .toList();

    return new Snapshot(failedCount, staleCount + neverRunCount, neverRunCount, true, List.copyOf(items));
  }

  private static String severity(String status) {
    if ("FAILED".equals(status) || "ABANDONED".equals(status)) {
      return "CRITICAL";
    }
    if ("STOPPED".equals(status) || "STOPPING".equals(status) || "UNKNOWN".equals(status)) {
      return "WARNING";
    }
    return "OK";
  }

  private static int severityRank(String severity) {
    return switch (severity) {
      case "CRITICAL" -> 0;
      case "WARNING" -> 1;
      case "OK" -> 2;
      default -> 1;
    };
  }

  private record ExecutionItem(java.time.Instant occurredAt, Item item) {}
}
