package com.tchalanet.server.core.session.application.query.model;
import com.tchalanet.server.common.types.id.SessionId;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.TenantId;
import java.util.Optional;
import java.util.UUID;

import com.tchalanet.server.core.session.domain.model.PosSessionTotals;

public record GetSessionTotalsQuery(TenantId tenantId, SessionId sessionId) implements Query<Optional<PosSessionTotals>> {
}
