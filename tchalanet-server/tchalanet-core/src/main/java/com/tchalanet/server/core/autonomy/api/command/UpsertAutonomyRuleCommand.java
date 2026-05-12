package com.tchalanet.server.core.autonomy.api.command;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.enums.ApprovalRole;
import com.tchalanet.server.common.types.enums.AutonomyLevel;
import com.tchalanet.server.common.types.enums.AutonomyTargetType;
import com.tchalanet.server.common.types.id.AutonomyPolicyRuleId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.autonomy.internal.domain.model.AutonomyTargetId;

import java.time.Instant;

public record UpsertAutonomyRuleCommand(
    TenantId tenantId,
    AutonomyTargetType targetType,
    AutonomyTargetId targetId,
    AutonomyLevel level,
    boolean requireApprovalOnBlock,
    ApprovalRole approvalRole,
    boolean enabled,
    Instant startsAt,
    Instant endsAt
) implements Command<AutonomyPolicyRuleId> {
}
