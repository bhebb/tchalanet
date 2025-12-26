package com.tchalanet.server.core.draw.application.port.out;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.draw.application.query.projection.DrawChannelCalendarRow;
import java.util.List;

public interface DrawChannelQueryPort {
  List<DrawChannelCalendarRow> listActiveCalendarRows(TenantId tenantId);
}
