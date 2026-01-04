package com.tchalanet.server.core.draw.application.port.out;

import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.draw.application.query.model.DrawChannelSearchCriteria;
import com.tchalanet.server.core.draw.application.query.projection.DrawChannelCalendarRow;
import com.tchalanet.server.core.draw.domain.model.DrawChannel;
import com.tchalanet.server.core.draw.domain.model.DrawChannelSummary;
import java.util.List;
import java.util.Optional;

public interface DrawChannelReaderPort {
  Optional<DrawChannel> findById(TenantId tenantId, DrawChannelId id);

  Optional<DrawChannel> findByCode(TenantId tenantId, String code);

  List<DrawChannel> findActiveByTenant(TenantId tenantId);

  List<DrawChannelSummary> findByCriteria(DrawChannelSearchCriteria criteria);

  List<DrawChannelCalendarRow> listActiveCalendarRows(TenantId tenantId);
}
