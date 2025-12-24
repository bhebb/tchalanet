package com.tchalanet.server.core.autonomy.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents an autonomy policy rule that defines the approval requirements
 * for transactions based on the target entity (TENANT, OUTLET, TERMINAL, AGENT).
 *
 * An autonomy policy determines whether certain transactions require approval
 * and which role can provide that approval.
 */
public record AutonomyPolicyRuleRule(
    UUID id,
    UUID tenantId,
    AutonomyTargetType targetType,
    UUID targetId,
    AutonomyLevel level,
    boolean requireApprovalOnBlock,
    ApprovalRole approvalRole,
    boolean enabled,
    Instant startsAt,
    Instant endsAt,
    long version
) {
    public boolean isActiveAt(Instant now) {
        if (!enabled) return false;
        if (startsAt != null && now.isBefore(startsAt)) return false;
        if (endsAt != null && !now.isBefore(endsAt)) return false;
        return true;
    }
}
