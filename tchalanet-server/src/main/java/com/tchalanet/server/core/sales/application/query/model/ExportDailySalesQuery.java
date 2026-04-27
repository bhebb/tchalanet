package com.tchalanet.server.core.sales.application.query.model;

import com.tchalanet.server.common.types.id.TenantId;
import java.time.LocalDate;

/** Query to export daily sales (e.g. CSV). */
public record ExportDailySalesQuery(TenantId tenantId, LocalDate date) {}
