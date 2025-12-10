package com.tchalanet.server.core.sales.application.command.model;

import java.time.Instant;
import java.util.UUID;

/** Command to archive old tickets. */
public record ArchiveTicketsCommand(
    UUID tenantId,
    Instant cutoffDate
) {}
