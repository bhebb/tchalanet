package com.tchalanet.server.tenant.domain.ports;

import com.tchalanet.server.tenant.domain.model.Theme;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ThemeRepository {
  Optional<Theme> findById(UUID id);

  List<Theme> findByTenantId(UUID tenantId);

  Optional<Theme> findFirstPublished(UUID tenantId);
}
