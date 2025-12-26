package com.tchalanet.server.core.autonomy.application.command.model;
import com.tchalanet.server.common.types.enums.ApprovalRole;
import com.tchalanet.server.common.types.enums.AutonomyLevel;
import com.tchalanet.server.common.types.enums.AutonomyTargetType;
import com.tchalanet.server.common.types.id.TenantId;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.core.autonomy.domain.model.AutonomyPolicyRule;

import java.time.Instant;
import java.util.UUID;

public record UpsertAutonomyPolicyRuleCommand(
    TenantId tenantId,
    AutonomyTargetType targetType,
    UUID targetId,
    AutonomyLevel level,
    boolean requireApprovalOnBlock,
    ApprovalRole approvalRole,     // nullable
    boolean enabled,
    Instant startsAt,              // nullable
    Instant endsAt                 // nullable
) implements Command<AutonomyPolicyRule> {}
