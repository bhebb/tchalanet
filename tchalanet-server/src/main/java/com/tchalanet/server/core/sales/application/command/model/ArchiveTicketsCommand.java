package com.tchalanet.server.core.sales.application.command.model;

import com.tchalanet.server.common.types.id.TenantId;
import java.time.Instant;

/** Command to archive old tickets. */
public record ArchiveTicketsCommand(TenantId tenantId, Instant cutoffDate) {}
