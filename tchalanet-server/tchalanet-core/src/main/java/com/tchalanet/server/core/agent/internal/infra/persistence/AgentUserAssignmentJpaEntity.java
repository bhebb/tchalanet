package com.tchalanet.server.core.agent.internal.infra.persistence;

import com.tchalanet.server.common.persistence.BaseTenantEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name="agent_user_assignment", uniqueConstraints=@UniqueConstraint(name="uq_agent_user_relation", columnNames={"tenant_id","agent_id","user_id","relation"}))
@Getter
@Setter
public class AgentUserAssignmentJpaEntity extends BaseTenantEntity {
  @Column(name="agent_id", nullable=false)
  private UUID agentId;

  @Column(name="user_id", nullable=false)
  private UUID userId;

  @Column(nullable=false, length=32)
  private String relation;

  @Column(nullable=false)
  private boolean active;
}
