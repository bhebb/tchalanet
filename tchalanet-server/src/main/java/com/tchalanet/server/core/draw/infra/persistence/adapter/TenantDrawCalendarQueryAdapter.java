package com.tchalanet.server.core.draw.infra.persistence.adapter;

import com.tchalanet.server.catalog.tenant.api.TenantCatalog;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.draw.application.port.out.TenantDrawCalendarQueryPort;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TenantDrawCalendarQueryAdapter implements TenantDrawCalendarQueryPort {

  private final TenantCatalog tenantCatalog;

  @Override
  public List<TenantId> listActiveTenantIdsForDrawCalendar() {
    // Reuse existing tenant catalog to get active tenant ids
    return tenantCatalog.listActiveTenantIds();
  }
}
