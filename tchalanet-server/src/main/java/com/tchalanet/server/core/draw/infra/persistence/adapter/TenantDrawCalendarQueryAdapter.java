package com.tchalanet.server.core.draw.infra.persistence.adapter;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.draw.application.port.out.TenantDrawCalendarQueryPort;
import com.tchalanet.server.core.tenant.application.port.out.TenantReaderPort;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TenantDrawCalendarQueryAdapter implements TenantDrawCalendarQueryPort {

  private final TenantReaderPort tenantReaderPort;

  @Override
  public List<TenantId> listActiveTenantIdsForDrawCalendar() {
    // Reuse existing tenant reader to get active tenant ids
    return tenantReaderPort.listActiveTenantIds();
  }
}
