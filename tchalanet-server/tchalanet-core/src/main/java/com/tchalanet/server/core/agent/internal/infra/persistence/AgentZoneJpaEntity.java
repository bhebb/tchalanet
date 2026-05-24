package com.tchalanet.server.core.agent.internal.infra.persistence;

import com.tchalanet.server.common.persistence.BaseTenantEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "agent_zone", uniqueConstraints = @UniqueConstraint(name = "uq_agent_zone_tenant_code", columnNames = { "tenant_id", "code" }))
@Getter
@Setter
public class AgentZoneJpaEntity extends BaseTenantEntity {
  @Column(name="parent_zone_id")
  private UUID parentZoneId;
  @Column(nullable=false, length=80)
  private String code;
  @Column(nullable=false, length=160)
  private String name;
  @Column(name="zone_type", nullable=false, length=40)
  private String zoneType;
  @Column(nullable=false, length=24)
  private String status;
  @Column(nullable=false)
  private int depth;
  @Column(name="created_at", nullable=false)
  private Instant createdAt;
  @Column(name="updated_at", nullable=false)
  private Instant updatedAt;
}
