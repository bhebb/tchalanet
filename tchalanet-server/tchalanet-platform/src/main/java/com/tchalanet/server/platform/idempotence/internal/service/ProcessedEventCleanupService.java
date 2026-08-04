package com.tchalanet.server.platform.idempotence.internal.service;

import com.tchalanet.server.common.context.TchContextScope;
import com.tchalanet.server.platform.tenant.api.TenantPreContextLookupApi;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/** Removes expired idempotency markers from {@code processed_event}. */
@Service
@Slf4j
public class ProcessedEventCleanupService {

  private final JdbcTemplate jdbc;
  private final Clock clock;
  private final Duration retention;
  private final TenantPreContextLookupApi tenantRegistry;

  public ProcessedEventCleanupService(
      JdbcTemplate jdbc,
      Clock clock,
      @Value("${tch.idempotence.processed-event-cleanup.retention:P30D}") Duration retention,
      TenantPreContextLookupApi tenantRegistry) {
    this.jdbc = jdbc;
    this.clock = clock;
    this.retention = retention;
    this.tenantRegistry = tenantRegistry;
  }

  public int cleanupExpired() {
    Instant cutoff = Instant.now(clock).minus(retention);
    var activeTenantIds = tenantRegistry.listActiveTenantIds();
    int deleted = 0;
    for (var tenantId : activeTenantIds) {
      deleted +=
          TchContextScope.runWithTemporaryTenantResult(
              tenantId.value(),
              "processed-event-cleanup",
              () ->
                  jdbc.update(
                      """
                      DELETE FROM processed_event
                       WHERE processed_at < ?
                      """,
                      cutoff));
    }
    log.info(
        "processed_event cleanup: cutoff={} deleted={} tenants={}",
        cutoff,
        deleted,
        activeTenantIds.size());
    return deleted;
  }
}
