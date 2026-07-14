package com.tchalanet.server.platform.tenant.api;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.platform.tenant.api.model.TenantContextLookupView;
import com.tchalanet.server.platform.tenant.api.model.TenantStatsView;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;

/**
 * Read-only tenant registry API — single interface for all tenant reads outside RLS.
 *
 * <p>Allowed callers:
 *
 * <ul>
 *   <li>Auth/bootstrap — resolve tenant before {@code TchRequestContext} is bound
 *   <li>Scheduler/batch — iterate active tenants
 *   <li>Platform-admin — registry listings
 *   <li>Internal platform services — zone, locale resolution
 * </ul>
 *
 * <p>Must NOT be called from sales, payout, settlement, or tenant-admin business screens.
 */
public interface TenantPreContextLookupApi {

  Optional<TenantContextLookupView> findByCode(String codeLower);

  Optional<TenantContextLookupView> findById(TenantId tenantId);

  List<TenantId> listActiveTenantIds();

  TchPage<TenantContextLookupView> listTenants(PageRequest pageRequest);

  TenantStatsView stats();
}
