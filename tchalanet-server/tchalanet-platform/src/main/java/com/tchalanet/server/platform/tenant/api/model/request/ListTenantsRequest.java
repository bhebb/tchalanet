package com.tchalanet.server.platform.tenant.api.model.request;

import com.tchalanet.server.common.web.paging.TchPage;
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
public record ListTenantsRequest(
    Pageable pageable  // from TchPageRequest
) {}
