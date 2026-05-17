package com.tchalanet.server.core.sales.api.model.lifecycle;

import com.tchalanet.server.common.types.id.ApprovalRequestId;
import com.tchalanet.server.common.types.id.UserId;

import java.time.Instant;

public record ApprovalTrace(
    ApprovalRequestId requestId,
    UserId requestedBy,
    Instant requestedAt,
    Instant approvedAt,
    UserId approvedBy
) {}
