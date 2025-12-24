package com.tchalanet.server.core.autonomy.domain;

import com.tchalanet.server.core.autonomy.domain.model.AutonomyLevel;
import com.tchalanet.server.core.autonomy.domain.model.ApprovalRole;

/**
 * Resolved autonomy information for a specific transaction context.
 *
 * Contains the effective autonomy level and approval requirements
 * after evaluating the applicable autonomy policies.
 */
public record ResolvedAutonomy(
    AutonomyLevel level,
    boolean requireApprovalOnBlock,
    ApprovalRole approvalRole
) {}
