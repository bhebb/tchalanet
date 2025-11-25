package com.tchalanet.server.common.audit.infra.persistence;

import com.tchalanet.server.common.infra.persistence.BaseTenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "audit_event")
@Getter
@Setter
@NoArgsConstructor
public class AuditEventJpaEntity extends BaseTenantEntity {

  @Column(name = "occurred_at", nullable = false)
  private Instant occurredAt;

  @Column(name = "actor_type", nullable = false, length = 32)
  private String actorType;

  @Column(name = "actor_id", nullable = false)
  private String actorId;

  @Column(name = "entity_type", nullable = false, length = 64)
  private String entityType;

  @Column(name = "entity_id", nullable = false)
  private String entityId;

  @Column(name = "action", nullable = false, length = 64)
  private String action;

  @Column(name = "details", nullable = false, columnDefinition = "jsonb")
  private String details;

  @Column(name = "ip", columnDefinition = "inet")
  private String ip;

  @Column(name = "user_agent")
  private String userAgent;
}
