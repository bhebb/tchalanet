package com.tchalanet.server.core.draw.application.port.out;

import java.util.List;
import java.util.UUID;

public interface TenantDrawCalendarQueryPort {
  List<UUID> listActiveTenantIdsForDrawCalendar();
}

