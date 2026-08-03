package com.tchalanet.server.core.analytics.internal.infra.persistence;

import com.tchalanet.server.common.types.id.TenantId;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Serializes one tenant's analytics projection writes with an operational reconciliation run.
 *
 * <p>The advisory lock is transaction-scoped. Callers must therefore invoke it inside their
 * existing transaction before recording the idempotency marker or mutating a projection.
 */
@Component
@RequiredArgsConstructor
public class AnalyticsTenantProjectionLock {

  private static final String LOCK_NAMESPACE = "analytics-projection:";

  private final JdbcTemplate jdbc;

  public void acquire(TenantId tenantId) {
    jdbc.query(
        "SELECT pg_advisory_xact_lock(hashtextextended(?, 0))",
        resultSet -> {},
        LOCK_NAMESPACE + tenantId.value());
  }
}
