package com.tchalanet.server.platform.tenantconfig.api.model;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.paging.TchPage;
import org.springframework.data.domain.Pageable;

/**
 * Query: List all tenants with pagination.
 * Per command_query_handlers.md + pagination.md:
 * - Implements Query<TchPage<TenantConfigView>>
 * - Used by platform admins to browse all tenants
 * - Superadmins see all, tenant admins may see filtered (policy-defined)
 * - Returns TchPage (not Spring Page) per pagination.md
 * - Accepts Pageable (from @TchPaging TchPageRequest)
 */
public record ListTenantsQuery(
    Pageable pageable  // from TchPageRequest
) implements Query<TchPage<TenantConfigView>> {}
