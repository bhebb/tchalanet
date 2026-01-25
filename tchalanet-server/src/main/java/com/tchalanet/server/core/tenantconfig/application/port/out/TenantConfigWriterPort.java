package com.tchalanet.server.core.tenantconfig.application.port.out;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.tenantconfig.domain.model.TenantConfig;

/**
 * Output port: write tenant configuration.
 * Per DOMAIN_TENANT_CONFIG.md:
 * - Separate create and update operations for explicit intent
 */
public interface TenantConfigWriterPort {

  /**
   * Create a new tenant (INSERT only).
   * May throw DataIntegrityViolationException on unique constraint violation.
   * @return the created tenant
   */
  TenantConfig create(TenantConfig tenant);

  /**
   * Update an existing tenant (UPDATE only).
   * @throws jakarta.persistence.EntityNotFoundException if tenant does not exist
   * @return the updated tenant
   */
  TenantConfig update(TenantConfig tenant);

}
