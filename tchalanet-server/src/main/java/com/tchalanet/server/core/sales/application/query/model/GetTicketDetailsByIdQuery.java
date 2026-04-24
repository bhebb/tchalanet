package com.tchalanet.server.core.sales.application.query.model;

import java.util.UUID;

/** Query to get ticket details by ID. */
public record GetTicketDetailsByIdQuery(UUID id) {
}
