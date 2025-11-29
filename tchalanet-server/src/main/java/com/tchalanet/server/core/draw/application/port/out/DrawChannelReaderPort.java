package com.tchalanet.server.core.draw.application.port.out;

import com.tchalanet.server.core.draw.application.query.model.DrawChannelSearchCriteria;
import com.tchalanet.server.core.draw.domain.model.DrawChannel;
import com.tchalanet.server.core.draw.domain.model.DrawChannelId;
import com.tchalanet.server.core.draw.domain.model.DrawChannelSummary;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DrawChannelReaderPort {
  Optional<DrawChannel> findById(UUID tenantId, DrawChannelId id);

  Optional<DrawChannel> findByCode(UUID tenantId, String code);

  List<DrawChannel> findActiveByTenant(UUID tenantId);

  List<DrawChannelSummary> findByCriteria(DrawChannelSearchCriteria criteria);
}
