package com.tchalanet.server.core.sales.api.query;

import com.tchalanet.server.common.types.id.TenantId;
import java.time.LocalDate;

/** Query to export daily sales (e.g. CSV). */
public record ExportDailySalesQuery(TenantId tenantId, LocalDate date) {}
