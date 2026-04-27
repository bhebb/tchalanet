package com.tchalanet.server.core.tenanttheme.application.port.out;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.tenanttheme.domain.model.TenantTheme;
import java.util.Optional;

/**
 * Port for reading tenant theme.
 * Maps to spec requirement T6.
 */
public interface TenantThemeReaderPort {

  /**
   * Find the effective tenant theme.
   * Must enforce RLS at the adapter level.
   */
  Optional<TenantTheme> findByTenantId(TenantId tenantId);
}
