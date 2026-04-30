package com.tchalanet.server.common.idempotency.event;

import com.tchalanet.server.common.context.TchContextResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ProcessedEventJdbcAdapter implements ProcessedEventPort {

  private final JdbcTemplate jdbc;
  private final TchContextResolver ctxResolver;

  @Override
  public boolean alreadyProcessed(String handlerKey, UUID eventId) {
    Integer found = jdbc.queryForObject("""
        SELECT COUNT(1)
        FROM processed_event
        WHERE handler_key = ?
          AND event_id = ?
        """, Integer.class, handlerKey, eventId);
    return found != null && found > 0;
  }

  @Override
  public void markProcessed(String handlerKey, UUID eventId) {
    markProcessedIfAbsent(handlerKey, eventId);
  }

  @Override
  public boolean markProcessedIfAbsent(String handlerKey, UUID eventId) {
    var ctx = ctxResolver.currentOrThrow();
    var tenantId = ctx.tenantId().value();
    UUID createdBy = null;
    if (ctx.userUuid() != null) createdBy = ctx.userUuid();

    int inserted = jdbc.update("""
        INSERT INTO processed_event (tenant_id, handler_key, event_id, created_by)
        VALUES (?, ?, ?, ?)
        ON CONFLICT (tenant_id, handler_key, event_id) DO NOTHING
        """, tenantId, handlerKey, eventId, createdBy);
    return inserted > 0;
  }
}
