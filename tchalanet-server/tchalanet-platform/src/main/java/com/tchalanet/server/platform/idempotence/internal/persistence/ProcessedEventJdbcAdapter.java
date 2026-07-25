package com.tchalanet.server.platform.idempotence.internal.persistence;

import com.tchalanet.server.common.constant.CommonConstants;
import com.tchalanet.server.common.context.TchContextResolver;
import com.tchalanet.server.platform.idempotence.api.ProcessedEventPort;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
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
    UUID createdBy = null;
    if (ctx.userUuid() != null) createdBy = ctx.userUuid();

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
}
