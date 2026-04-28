package com.tchalanet.server.core.session.application.command.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.SessionId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.session.domain.model.SalesSessionTotals;

/** Command to recompute totals for a POS session. */
public record RecomputeSalesSessionTotalsCommand(TenantId tenantId, SessionId sessionId)
    implements Command<SalesSessionTotals> {}
