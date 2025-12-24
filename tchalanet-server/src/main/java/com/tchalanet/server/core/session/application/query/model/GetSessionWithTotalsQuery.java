package com.tchalanet.server.core.session.application.query.model;

import com.tchalanet.server.common.bus.Query;
import java.util.UUID;

/**
 * Query to get a session with its totals.
 */
public record GetSessionWithTotalsQuery(UUID sessionId) implements Query<SessionWithTotalsDto> {}
