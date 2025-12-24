package com.tchalanet.server.core.session.application.query.model;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.core.session.domain.model.PosSessionTotals;

import java.util.Optional;
import java.util.UUID;

public record GetSessionTotalsQuery(UUID tenantId, UUID sessionId) implements Query<Optional<PosSessionTotals>> {
}
