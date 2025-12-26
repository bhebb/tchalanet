package com.tchalanet.server.core.session.application.query.model;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.SessionId;

/** Query to get a session with its totals. */
public record GetSessionWithTotalsQuery(SessionId sessionId)
    implements Query<SessionWithTotalsDto> {}
