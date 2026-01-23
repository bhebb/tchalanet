package com.tchalanet.server.catalog.theme.application.port.out;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.catalog.theme.domain.model.Theme;
import com.tchalanet.server.catalog.theme.domain.model.ThemeStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ThemeReaderPort {

  Optional<Theme> findById(UUID id);

  Optional<Theme> findPublishedById(UUID id);

  List<Theme> listByTenantAndStatus(TenantId tenantId, ThemeStatus status);

  Optional<Theme> findActiveForTenant(TenantId tenantId);
}
