package com.tchalanet.server.core.draw.application.port.out;
import com.tchalanet.server.common.types.id.TenantId;

import java.util.List;
import java.util.UUID;

public interface TenantDrawCalendarQueryPort {
  List<TenantId> listActiveTenantIdsForDrawCalendar();
}

