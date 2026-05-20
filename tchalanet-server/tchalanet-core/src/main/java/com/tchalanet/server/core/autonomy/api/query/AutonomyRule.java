package com.tchalanet.server.core.autonomy.api.query;

import com.tchalanet.server.platform.identity.api.model.AutonomyLevel;
import com.tchalanet.server.core.autonomy.internal.domain.model.ApprovalRole;
import java.time.Instant;

public record AutonomyRule(
    AutonomyLevel level,
    boolean requireApprovalOnBlock,
    ApprovalRole approvalRole,
    boolean enabled,
    Instant startsAt,
    Instant endsAt) {}
