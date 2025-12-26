package com.tchalanet.server.core.session.application.query.model;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.core.session.domain.model.PosSession;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import java.util.Optional;

/**
 * Query to get the current open session for a terminal.
 */
public record GetCurrentSessionQuery(
    TenantId tenantId,
    TerminalId terminalId
) implements Query<Optional<PosSession>> {}
