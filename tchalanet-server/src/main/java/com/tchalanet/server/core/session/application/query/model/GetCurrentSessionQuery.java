package com.tchalanet.server.core.session.application.query.model;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.core.session.domain.model.PosSession;
import java.util.Optional;
import java.util.UUID;

/**
 * Query to get the current open session for a terminal.
 */
public record GetCurrentSessionQuery(
    UUID tenantId,
    UUID terminalId
) implements Query<Optional<PosSession>> {}
