package com.tchalanet.server.platform.audit.internal.persistence;

import com.tchalanet.server.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.net.InetAddress;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import tools.jackson.databind.JsonNode;

@Entity
@Table(name = "audit_event")
@Getter
@Setter
public class AuditEventJpaEntity extends BaseEntity {

  @Column(name = "tenant_id", columnDefinition = "uuid")
  private java.util.UUID tenantId;

  @Column(name = "occurred_at", nullable = false)
  private Instant occurredAt;

  @Column(name = "actor_type", nullable = false, length = 32)
  private String actorType;

  @Column(name = "actor_id")
  private String actorId;

  @Column(name = "entity_type", nullable = false, length = 64)
  private String entityType;

  @Column(name = "entity_id", nullable = false)
  private String entityId;

  @Column(name = "action", nullable = false, length = 64)
  private String action;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "details", columnDefinition = "jsonb")
  private JsonNode details;

  @Column(name = "ip", columnDefinition = "inet")
  @JdbcTypeCode(SqlTypes.INET)
  private InetAddress ip;

  @Column(name = "user_agent")
  private String userAgent;
}
