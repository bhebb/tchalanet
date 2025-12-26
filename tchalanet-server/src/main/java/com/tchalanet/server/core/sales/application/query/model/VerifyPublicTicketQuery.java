package com.tchalanet.server.core.sales.application.query.model;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.core.sales.domain.model.TicketVerificationResult;
import java.time.Instant;

/** Query to verify a ticket by its public code. */
public record VerifyPublicTicketQuery(String publicCode, Instant now)
    implements Query<TicketVerificationResult> {}
