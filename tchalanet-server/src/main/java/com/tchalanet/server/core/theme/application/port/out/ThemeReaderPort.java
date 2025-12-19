package com.tchalanet.server.core.theme.application.port.out;

import com.tchalanet.server.core.theme.domain.model.Theme;
import com.tchalanet.server.core.theme.domain.model.ThemeStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ThemeReaderPort {

  Optional<Theme> findById(UUID id);

  Optional<Theme> findPublishedById(UUID id);

  List<Theme> listByTenantAndStatus(UUID tenantId, ThemeStatus status);

  Optional<Theme> findActiveForTenant(UUID tenantId);
}
