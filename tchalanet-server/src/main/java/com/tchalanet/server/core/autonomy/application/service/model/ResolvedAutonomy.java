package com.tchalanet.server.core.autonomy.application.service.model;

import com.tchalanet.server.common.types.enums.ApprovalRole;
import com.tchalanet.server.common.types.enums.AutonomyLevel;
import com.tchalanet.server.common.types.enums.AutonomyTargetType;
import com.tchalanet.server.core.autonomy.domain.ids.AutonomyTargetId;
import java.util.UUID;

public record ResolvedAutonomy(
    AutonomyLevel level,
    boolean requireApprovalOnBlock,
    ApprovalRole approvalRole,
    Source source
) {
  public record Source(AutonomyTargetType targetType, AutonomyTargetId targetId, UUID ruleId, Long version, boolean isDefault) {}
}
