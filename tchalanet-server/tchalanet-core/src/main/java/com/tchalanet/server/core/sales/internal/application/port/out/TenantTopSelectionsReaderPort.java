package com.tchalanet.server.core.sales.internal.application.port.out;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.sales.api.query.TenantTopSelectionsByPeriodView;
import java.time.LocalDate;
import java.time.ZoneId;

public interface TenantTopSelectionsReaderPort {

  TenantTopSelectionsByPeriodView findTopSelectionsByPeriod(
      TenantId tenantId, LocalDate from, LocalDate to, ZoneId zoneId, int limit);
}
