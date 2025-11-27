package com.tchalanet.server.draw.application.ports.in;

import com.tchalanet.server.draw.domain.model.DrawChannel;
import com.tchalanet.server.draw.domain.model.DrawChannelId;
import com.tchalanet.server.tenant.domain.model.TenantId;
import java.util.List;

public interface DrawChannelCrudUseCase {
  DrawChannel create(DrawChannel d);

  DrawChannel update(DrawChannel d);

  void delete(DrawChannelId id);

  List<DrawChannel> listByTenant(TenantId tenantId);
}
