package com.tchalanet.server.core.autonomy.application.command.model;

import com.tchalanet.server.common.types.enums.AutonomyLevel;
import com.tchalanet.server.common.types.enums.ApprovalRole;
import com.tchalanet.server.common.types.enums.AutonomyTargetType;
import com.tchalanet.server.core.autonomy.domain.ids.AutonomyTargetId;
import java.time.OffsetDateTime;

public class UpsertAutonomyPolicyRuleCommand {

  private final AutonomyTargetType targetType;
  private final AutonomyTargetId targetId;

  private final AutonomyLevel level;
  private final boolean requireApprovalOnBlock;
  private final ApprovalRole approvalRole; // nullable

  private final boolean enabled;
  private final OffsetDateTime startsAt;
  private final OffsetDateTime endsAt;

  private final Long expectedVersion;

  public UpsertAutonomyPolicyRuleCommand(
      AutonomyTargetType targetType,
      AutonomyTargetId targetId,
      AutonomyLevel level,
      boolean requireApprovalOnBlock,
      ApprovalRole approvalRole,
      boolean enabled,
      OffsetDateTime startsAt,
      OffsetDateTime endsAt,
      Long expectedVersion) {
    this.targetType = targetType;
    this.targetId = targetId;
    this.level = level;
    this.requireApprovalOnBlock = requireApprovalOnBlock;
    this.approvalRole = approvalRole;
    this.enabled = enabled;
    this.startsAt = startsAt;
    this.endsAt = endsAt;
    this.expectedVersion = expectedVersion;
  }

  public AutonomyTargetType getTargetType() {
    return targetType;
  }

  public AutonomyTargetId getTargetId() {
    return targetId;
  }

  public AutonomyLevel getLevel() {
    return level;
  }

  public boolean isRequireApprovalOnBlock() {
    return requireApprovalOnBlock;
  }

  public ApprovalRole getApprovalRole() {
    return approvalRole;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public OffsetDateTime getStartsAt() {
    return startsAt;
  }

  public OffsetDateTime getEndsAt() {
    return endsAt;
  }

  public Long getExpectedVersion() {
    return expectedVersion;
  }
}
