package com.tchalanet.server.platform.archive.internal.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import org.springframework.stereotype.Component;

/**
 * Micrometer metrics for the archive system.
 *
 * <p>All counters and timers are eagerly registered at construction time so they appear
 * in the metrics endpoint with zero values even before the first archive run.
 */
@Component
public class ArchiveMetrics {

  // ── Run-level ───────────────────────────────────────────────────────────────

  private final Counter runsCompleted;
  private final Counter runsFailed;
  private final Timer runDuration;

  // ── Dataset-level (recorded per provider per run) ──────────────────────────

  private final MeterRegistry registry;

  // ── Lookup/read path ───────────────────────────────────────────────────────

  private final Counter lookupFallbacks;
  private final Counter objectReadErrors;

  public ArchiveMetrics(MeterRegistry registry) {
    this.registry = registry;

    runsCompleted = Counter.builder("archive.runs.total")
        .tag("status", "COMPLETED")
        .description("Total archive runs completed successfully")
        .register(registry);

    runsFailed = Counter.builder("archive.runs.total")
        .tag("status", "FAILED")
        .description("Total archive runs that failed")
        .register(registry);

    runDuration = Timer.builder("archive.run.duration")
        .description("Duration of an archive run")
        .register(registry);

    lookupFallbacks = Counter.builder("archive.lookup.fallback.total")
        .description("Times archive lookup was used because entity not found in hot table")
        .register(registry);

    objectReadErrors = Counter.builder("archive.object.read.errors.total")
        .description("Errors reading archive objects from storage")
        .register(registry);
  }

  // ── Recording methods ───────────────────────────────────────────────────────

  public void recordRunCompleted(Duration duration) {
    runsCompleted.increment();
    runDuration.record(duration);
  }

  public void recordRunFailed() {
    runsFailed.increment();
  }

  public void recordRowsExported(String dataset, long rows) {
    Counter.builder("archive.rows.exported.total")
        .tag("dataset", dataset)
        .description("Rows exported to archive object storage")
        .register(registry)
        .increment(rows);
  }

  public void recordBytesExported(String dataset, long bytes) {
    Counter.builder("archive.bytes.exported.total")
        .tag("dataset", dataset)
        .description("Bytes written to archive object storage (compressed)")
        .register(registry)
        .increment(bytes);
  }

  public void recordLookupFallback() {
    lookupFallbacks.increment();
  }

  public void recordObjectReadError(String tableName) {
    Counter.builder("archive.object.read.errors.total")
        .tag("table", tableName)
        .register(registry)
        .increment();
  }
}
