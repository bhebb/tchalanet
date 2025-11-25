package com.tchalanet.server.draw.domain.ports;

import com.tchalanet.server.common.domain.DrawChannelId;
import com.tchalanet.server.common.domain.TenantId;
import com.tchalanet.server.draw.domain.model.DrawChannel;
import java.util.List;
import java.util.Optional;

public interface DrawChannelRepository {
  DrawChannel save(DrawChannel d);

  Optional<DrawChannel> findById(DrawChannelId id);

  List<DrawChannel> findByTenant(TenantId tenantId);

  List<DrawChannel> findAllActive();

  void deleteById(DrawChannelId id);
}
