package com.tchalanet.server.catalog.tenant.api;

import com.tchalanet.server.catalog.tenant.api.model.TenantBootstrapView;
import com.tchalanet.server.catalog.tenant.api.model.TenantRegistryView;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.catalog.tenant.api.model.TenantStatsView;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Read-only tenant catalog API.
 * Provides tenant registry and bootstrap lookup, bypassing RLS.
 * Per DOMAIN_TENANT_CATALOG.md:
 * - side-effect free
 * - never exposes JPA entities
 * - never depends on internal/*
 */
public interface TenantCatalog {

  /**
   * Find tenant ID by code (lowercase).
   * Used for tenant resolution before context is set.
   *
   * @param codeLower normalized tenant code
   * @return tenant ID if found and not deleted
   */
  Optional<TenantId> findIdByCode(String codeLower);

  /**
   * Find bootstrap view by code (lowercase).
   * Used to build TenantContextInfo during authentication/bootstrap.
   * Includes: tenantId, code, status, type, timezone, currency
   *
   * @param codeLower normalized tenant code
   * @return bootstrap view if found and not deleted
   */
  Optional<TenantBootstrapView> findBootstrapByCode(String codeLower);

  /**
   * Find bootstrap view by tenant ID.
   * Used for context setup.
   *
   * @param tenantId tenant identifier
   * @return bootstrap view if found and not deleted
   */
  Optional<TenantBootstrapView> findBootstrapById(TenantId tenantId);

  /**
   * Find registry view by tenant ID.
   * Used for admin listings/filters.
   *
   * @param tenantId tenant identifier
   * @return registry view if found and not deleted
   */
  Optional<TenantRegistryView> findRegistryById(TenantId tenantId);

  /**
   * Find registry view by code (lowercase).
   * Used for tenant lookups in listings.
   *
   * @param codeLower normalized tenant code
   * @return registry view if found and not deleted
   */
  Optional<TenantRegistryView> findRegistryByCode(String codeLower);

  /**
   * List all active tenant IDs.
   * Used for global filters/operations.
   *
   * @return list of active tenant IDs (status != ARCHIVED, not deleted)
   */
  List<TenantId> listActiveTenantIds();

  /**
   * List tenants with pagination (for admin listings).
   * Used by platform admins to browse all tenants.
   * Superadmins see all tenants, tenant admins may see filtered list (policy-defined).
   *
   * @param pageable pagination and sorting parameters
   * @return page of tenant registry views
   */
  Page<TenantRegistryView> listTenants(Pageable pageable);

  /**
   * Get tenant statistics.
   *
   * @return tenant statistics view
   */
  TenantStatsView stats();
}
