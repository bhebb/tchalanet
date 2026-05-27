package com.tchalanet.server.features.tenantadmin.readiness;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.features.tenantadmin.readiness.model.TenantReadinessView;

/**
 * Query returning the full readiness view for the current tenant.
 *
 * The effective tenant is read from {@code TchRequestContext} / RLS by the
 * handler — the query has no payload and never trusts a client-supplied
 * tenant id (dashboard-overview-runtime-v1 §spec tenant-readiness).
 *
 * Consumers:
 *   - tenant admin dashboard (consumes a {@link com.tchalanet.server.features.tenantadmin.readiness.model.TenantReadinessSummary}
 *     derived from the view)
 *   - tenant overview {@code GET /admin/overview}
 *   - tenant provisioning result
 */
public record GetTenantReadinessQuery() implements Query<TenantReadinessView> {}
