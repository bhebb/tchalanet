package com.tchalanet.server.core.session.application.command.model;
import com.tchalanet.server.common.types.id.SessionId;
import com.tchalanet.server.common.types.id.TenantId;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.core.session.application.query.model.GetSessionTotalsQuery;
import com.tchalanet.server.core.session.domain.model.PosSessionTotals;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Command to recompute totals for a POS session.
 */
public record RecomputePosSessionTotalsCommand(
    TenantId tenantId,
    SessionId sessionId
) implements Command<PosSessionTotals> {
}
