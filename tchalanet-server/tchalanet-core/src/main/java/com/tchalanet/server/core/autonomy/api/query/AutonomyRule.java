package com.tchalanet.server.core.autonomy.api.query;

import com.tchalanet.server.common.types.enums.AutonomyLevel;
import com.tchalanet.server.common.types.enums.ApprovalRole;
import java.time.Instant;

public record AutonomyRule(
    AutonomyLevel level,
    boolean requireApprovalOnBlock,
    ApprovalRole approvalRole,
    boolean enabled,
    Instant startsAt,
    Instant endsAt) {}
