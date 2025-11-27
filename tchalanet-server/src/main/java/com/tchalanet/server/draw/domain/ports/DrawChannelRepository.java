package com.tchalanet.server.draw.domain.ports;

import com.tchalanet.server.draw.domain.model.DrawChannel;
import com.tchalanet.server.draw.domain.model.DrawChannelId;
import com.tchalanet.server.tenant.domain.model.TenantId;
import java.util.List;
import java.util.Optional;

public interface DrawChannelRepository {
  DrawChannel save(DrawChannel d);

  Optional<DrawChannel> findById(DrawChannelId id);

  List<DrawChannel> findByTenant(TenantId tenantId);

  List<DrawChannel> findAllActive();

  void deleteById(DrawChannelId id);
}
