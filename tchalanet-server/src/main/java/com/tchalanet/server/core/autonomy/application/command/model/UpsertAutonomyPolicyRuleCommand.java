package com.tchalanet.server.core.autonomy.application.command.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.core.autonomy.domain.model.ApprovalRole;
import com.tchalanet.server.core.autonomy.domain.model.AutonomyLevel;
import com.tchalanet.server.core.autonomy.domain.model.AutonomyPolicyRuleRule;
import com.tchalanet.server.core.autonomy.domain.model.AutonomyTargetType;

import java.time.Instant;
import java.util.UUID;

public record UpsertAutonomyPolicyRuleRuleCommand(
    UUID tenantId,
    AutonomyTargetType targetType,
    UUID targetId,
    AutonomyLevel level,
    boolean requireApprovalOnBlock,
    ApprovalRole approvalRole,     // nullable
    boolean enabled,
    Instant startsAt,              // nullable
    Instant endsAt                 // nullable
) implements Command<AutonomyPolicyRuleRule> {}
