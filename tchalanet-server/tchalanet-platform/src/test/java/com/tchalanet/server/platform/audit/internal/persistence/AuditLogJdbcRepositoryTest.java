package com.tchalanet.server.platform.audit.internal.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.platform.audit.api.model.AuditAction;
import com.tchalanet.server.platform.audit.api.model.AuditActorType;
import com.tchalanet.server.platform.audit.api.model.AuditEntityType;
import com.tchalanet.server.platform.audit.internal.service.AuditEvent;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

class AuditLogJdbcRepositoryTest {

  private final NamedParameterJdbcTemplate jdbc =
      org.mockito.Mockito.mock(NamedParameterJdbcTemplate.class);
  private final AuditLogJdbcRepository repository = new AuditLogJdbcRepository(jdbc);

  @Test
  void saveWritesTheCanonicalAuditLogTable() {
    var tenantId = UUID.randomUUID();
    var actorId = UUID.randomUUID();
    var event =
        AuditEvent.of(
            TenantId.of(tenantId),
            Instant.parse("2026-08-04T10:15:30Z"),
            AuditActorType.USER,
            actorId,
            AuditEntityType.TICKET,
            "ticket-42",
            AuditAction.CREATE,
            "{\"outcome\":\"SUCCESS\"}",
            "127.0.0.1",
            "e2e-test");

    var sql = ArgumentCaptor.forClass(String.class);
    var parameters = ArgumentCaptor.forClass(MapSqlParameterSource.class);
    var saved = repository.save(event);

    verify(jdbc).update(sql.capture(), parameters.capture());
    assertThat(sql.getValue()).contains("INSERT INTO audit_log");
    assertThat(parameters.getValue().getValue("tenantId")).isEqualTo(tenantId);
    assertThat(parameters.getValue().getValue("actorId")).isEqualTo(actorId);
    assertThat(parameters.getValue().getValue("entityId")).isEqualTo("ticket-42");
    assertThat(saved.id()).isNotNull();
  }

  @Test
  void deleteBeforeDeletesOnlyFromTheCanonicalAuditLogTable() {
    when(jdbc.update(anyString(), any(MapSqlParameterSource.class))).thenReturn(3);
    var threshold = Instant.parse("2026-05-01T00:00:00Z");

    var deleted = repository.deleteBefore(threshold);

    assertThat(deleted).isEqualTo(3);
    var sql = ArgumentCaptor.forClass(String.class);
    verify(jdbc).update(sql.capture(), any(MapSqlParameterSource.class));
    assertThat(sql.getValue()).contains("DELETE FROM audit_log");
  }
}
