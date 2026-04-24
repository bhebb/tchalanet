package com.tchalanet.server.core.limitpolicy.infra.persistence.entity;

import com.tchalanet.server.common.persistence.BaseTenantEntity;
import com.tchalanet.server.common.types.enums.TargetType;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

@Entity
@Table(name = "limit_assignment")
@Getter
@Setter
@Audited
public class LimitAssignmentJpaEntity extends BaseTenantEntity {

  @Column(name = "limit_definition_id", nullable = false)
  private UUID limitDefinitionId;

  @Enumerated(EnumType.STRING)
  @Column(name = "target_type", nullable = false, length = 16)
  private TargetType targetType;

  @Column(name = "target_id")
  private UUID targetId;

  @Column(name = "enabled", nullable = false)
  private boolean enabled;

  @Column(name = "starts_at")
  private Instant startsAt;

  @Column(name = "ends_at")
  private Instant endsAt;
}
