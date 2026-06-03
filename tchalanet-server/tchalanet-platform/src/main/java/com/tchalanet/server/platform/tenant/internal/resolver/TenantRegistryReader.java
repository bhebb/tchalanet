package com.tchalanet.server.platform.tenant.internal.resolver;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Read-only pre-context tenant registry reader.
 *
 * <p>Bypasses RLS via {@code rawDataSource}. Allowed callers:
 * <ul>
 *   <li>Tenant resolution before {@code TchRequestContext} is bound (auth/bootstrap)</li>
 *   <li>Scheduler/batch active tenant listing</li>
 *   <li>Platform-admin registry listing</li>
 * </ul>
 *
 * <p>Must NOT be used by sales, payout, settlement, or tenant-admin business screens.
 */
public interface TenantRegistryReader {

    Optional<TenantBootstrapRow> findByCode(String codeLower);

    Optional<TenantBootstrapRow> findById(UUID tenantId);

    List<UUID> listActiveTenantIds();

    List<TenantBootstrapRow> listAll(int limit, int offset, String orderBy);

    long countAll();
}
