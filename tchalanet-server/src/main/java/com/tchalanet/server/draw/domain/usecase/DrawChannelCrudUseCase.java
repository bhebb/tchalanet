package com.tchalanet.server.draw.domain.usecase;

import com.tchalanet.server.draw.domain.model.DrawChannel;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DrawChannelCrudUseCase {
  DrawChannel create(DrawChannel d);

  Optional<DrawChannel> get(UUID id);

  List<DrawChannel> listAll();

  List<DrawChannel> listByTenant(UUID tenantId);

  DrawChannel update(UUID id, DrawChannel d);

  void delete(UUID id);
}
