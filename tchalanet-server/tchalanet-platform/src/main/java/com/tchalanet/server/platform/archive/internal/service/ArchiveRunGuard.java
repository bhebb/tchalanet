package com.tchalanet.server.platform.archive.internal.service;

import com.tchalanet.server.platform.archive.internal.persistence.ArchiveRunJdbcRepository;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Idempotency guard for archive runs.
 *
 * <p>Enforces the rule: same idempotency key must not create a second run when one is
 * COMPLETED. A FAILED run may be retried; a STARTED run is resumed.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ArchiveRunGuard {

  public enum Decision { CREATED, RESUMED, ALREADY_COMPLETED }

  public record GuardResult(UUID runId, Decision decision) {}

  private final ArchiveRunJdbcRepository runRepo;

  /**
   * Begin a new run or return an existing one for the given idempotency key.
   *
   * <ul>
   *   <li>No existing row → insert new run, return CREATED.</li>
   *   <li>Existing COMPLETED → no-op, return ALREADY_COMPLETED.</li>
   *   <li>Existing STARTED → return RESUMED (executor picks up where it left off).</li>
   *   <li>Existing FAILED → reset to STARTED, return RESUMED.</li>
   * </ul>
   */
  public GuardResult beginOrResume(String idempotencyKey, String strategy,
      String triggerType, UUID requestedBy, String reason) {

    Optional<Map<String, Object>> existing = runRepo.findByIdempotencyKey(idempotencyKey);

    if (existing.isPresent()) {
      Map<String, Object> run = existing.get();
      String status = (String) run.get("status");
      UUID runId = (UUID) run.get("id");

      if ("COMPLETED".equals(status)) {
        log.info("archive guard: run={} key={} already COMPLETED — skipping", runId, idempotencyKey);
        return new GuardResult(runId, Decision.ALREADY_COMPLETED);
      }
      if ("FAILED".equals(status)) {
        log.info("archive guard: run={} key={} was FAILED — resetting to STARTED", runId, idempotencyKey);
        runRepo.updateStatus(runId, "STARTED");
      } else {
        log.info("archive guard: run={} key={} resuming status={}", runId, status, idempotencyKey);
      }
      return new GuardResult(runId, Decision.RESUMED);
    }

    UUID runId = UUID.randomUUID();
    runRepo.insert(runId, "STARTED", strategy, triggerType, idempotencyKey, requestedBy, reason);
    log.info("archive guard: created run={} key={}", runId, idempotencyKey);
    return new GuardResult(runId, Decision.CREATED);
  }

  public void complete(UUID runId) {
    runRepo.complete(runId);
    log.info("archive guard: run={} COMPLETED", runId);
  }

  public void fail(UUID runId, String errorMessage) {
    runRepo.fail(runId, errorMessage);
    log.warn("archive guard: run={} FAILED: {}", runId, errorMessage);
  }
}
