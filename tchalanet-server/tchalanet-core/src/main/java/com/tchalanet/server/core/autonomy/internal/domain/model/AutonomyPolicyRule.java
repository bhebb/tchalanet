package com.tchalanet.server.core.autonomy.internal.domain.model;

import com.tchalanet.server.common.types.enums.ApprovalRole;
import com.tchalanet.server.common.types.enums.AutonomyLevel;
import com.tchalanet.server.common.types.enums.AutonomyTargetType;
import com.tchalanet.server.common.types.id.AutonomyPolicyRuleId;
import lombok.Builder;

import java.time.Instant;


public record AutonomyPolicyRule(
    AutonomyPolicyRuleId id,
    AutonomyTargetType targetType,
    AutonomyTargetId targetId,
    AutonomyLevel level,
    boolean requireApprovalOnBlock,
    ApprovalRole approvalRole,
    boolean enabled,
    Instant startsAt,
    Instant endsAt,
    Instant createdAt,
    Instant updatedAt,
    boolean deleted
) {
    public static AutonomyPolicyRule createNew(AutonomyPolicyRuleId id, AutonomyTargetType targetType, AutonomyTargetId of1, AutonomyLevel level, boolean requireApprovalOnBlock, ApprovalRole approvalRole, boolean enabled, Instant startsAt, Instant endsAt) {
        return new AutonomyPolicyRule(id, targetType, of1, level, requireApprovalOnBlock, approvalRole, enabled, startsAt, endsAt, null, null, false);
    }

    public static AutonomyPolicyRule update(AutonomyPolicyRuleId id, AutonomyTargetType autonomyTargetType, AutonomyTargetId autonomyTargetId, AutonomyLevel level, boolean requireApprovalOnBlock, ApprovalRole approvalRole, boolean enabled, Instant startsAt, Instant endsAt) {
        return new AutonomyPolicyRule(id, autonomyTargetType, autonomyTargetId, level, requireApprovalOnBlock, approvalRole, enabled, startsAt, endsAt, null, null, false);
    }
}
