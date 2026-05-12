package com.tchalanet.server.core.sales.api.query;

import java.util.UUID;

/** Query to get ticket details by ID. */
public record GetTicketDetailsByIdQuery(UUID id) {
}
