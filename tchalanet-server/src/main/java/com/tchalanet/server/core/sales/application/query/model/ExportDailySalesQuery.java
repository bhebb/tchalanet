package com.tchalanet.server.core.sales.application.query.model;

import java.util.UUID;
import java.time.LocalDate;

/** Query to export daily sales (e.g. CSV). */
public record ExportDailySalesQuery(
    UUID tenantId,
    LocalDate date
) {}

