package com.tchalanet.server.core.sales.application.query.model;

import com.tchalanet.server.common.types.id.TenantId;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Query to get ticket details by public code. */
public record GetTicketDetailsByPublicCodeQuery(String publicCode) {
}
