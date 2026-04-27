package com.tchalanet.server.core.draw.application.port.out;

import com.tchalanet.server.common.types.id.TenantId;
import java.util.List;

public interface TenantDrawCalendarQueryPort {
  List<TenantId> listActiveTenantIdsForDrawCalendar();
}
