package com.tchalanet.server.core.autonomy.internal.infra.persistence;

import com.tchalanet.server.common.types.enums.ApprovalRole;
import com.tchalanet.server.common.types.enums.AutonomyLevel;
import com.tchalanet.server.common.types.enums.AutonomyTargetType;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "autonomy_policy_rule")
@Getter
@Setter
public class AutonomyPolicyRuleJpaEntity extends BaseTenantEntity {

  @Column(name = "target_type", nullable = false)
  @Enumerated(EnumType.STRING)
  private AutonomyTargetType targetType;

  @Column(name = "target_id", nullable = false)
  private UUID targetId;

  @Column(name = "level", nullable = false)
  @Enumerated(EnumType.STRING)
  private AutonomyLevel level;

  @Column(name = "require_approval_on_block", nullable = false)
  private boolean requireApprovalOnBlock = true;

  @Column(name = "approval_role")
  @Enumerated(EnumType.STRING)
  private ApprovalRole approvalRole;

  @Column(name = "enabled", nullable = false)
  private boolean enabled = true;

  @Column(name = "starts_at")
  private Instant startsAt;

  @Column(name = "ends_at")
  private Instant endsAt;
}
