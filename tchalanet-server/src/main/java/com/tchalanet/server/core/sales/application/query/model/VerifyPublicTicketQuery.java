package com.tchalanet.server.core.sales.application.query.model;

import java.time.Instant;

/** Query to verify a ticket by its public code. */
public record VerifyPublicTicketQuery(
    String publicCode,
    Instant now
) {}

