package com.tchalanet.server.platform.idempotence.internal.persistence;

import com.tchalanet.server.common.constant.CommonConstants;
import com.tchalanet.server.common.context.TchContextResolver;
import com.tchalanet.server.common.context.TchContextScope;
import com.tchalanet.server.platform.idempotence.api.ProcessedEventPort;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProcessedEventJdbcAdapter implements ProcessedEventPort {

  private final JdbcTemplate jdbc;
  private final TchContextResolver ctxResolver;

  @Override
  public boolean alreadyProcessed(String handlerKey, UUID eventId) {
    Integer found =
        jdbc.queryForObject(
            """
        SELECT COUNT(1)
        FROM processed_event
        WHERE handler_key = ?
          AND event_id = ?
        """,
            Integer.class,
            handlerKey,
            eventId);
    return found != null && found > 0;
  }

  @Override
  public void markProcessed(String handlerKey, UUID eventId) {
    markProcessedIfAbsent(handlerKey, eventId);
  }

  @Override
  public boolean markProcessedIfAbsent(String handlerKey, UUID eventId) {
    var ctx = ctxResolver.currentOrThrow();
    // Global (platform-wide) events, e.g. draw results resolved by the background scheduler,
    // carry no tenant in context. Attribute idempotency tracking to the platform's own tenant
    // instead of failing, since processed_event requires a non-null tenant_id.
    var tenantId =
        ctx.tenantId() != null ? ctx.tenantId().value() : CommonConstants.DEFAULT_TENANT_UUID;
    UUID createdBy = ctx.userUuid();

    if (ctx.tenantId() == null) {
      return TchContextScope.runWithTemporaryTenantResult(
          tenantId,
          ctx.requestId(),
          () -> insertIfAbsentWithTemporaryRls(tenantId, handlerKey, eventId, createdBy));
    }

    return insertIfAbsent(tenantId, handlerKey, eventId, createdBy);
  }

  private boolean insertIfAbsentWithTemporaryRls(
      UUID tenantId, String handlerKey, UUID eventId, UUID createdBy) {
    setCurrentTenant(tenantId.toString());
    try {
      return insertIfAbsent(tenantId, handlerKey, eventId, createdBy);
    } finally {
      clearCurrentTenant();
    }
  }

  private boolean insertIfAbsent(UUID tenantId, String handlerKey, UUID eventId, UUID createdBy) {
    int inserted =
        jdbc.update(
            """
        INSERT INTO processed_event (tenant_id, handler_key, event_id, created_by)
        VALUES (?, ?, ?, ?)
        ON CONFLICT (tenant_id, handler_key, event_id) DO NOTHING
        """,
            tenantId,
            handlerKey,
            eventId,
            createdBy);
    return inserted > 0;
  }

  private void setCurrentTenant(String tenantId) {
    jdbc.queryForObject(
        "select set_config('app.current_tenant', ?, false)", String.class, tenantId);
  }

  private void clearCurrentTenant() {
    try {
      setCurrentTenant("");
    } catch (Exception e) {
      log.debug("processed_event temporary RLS reset failed", e);
    }
  }
}
