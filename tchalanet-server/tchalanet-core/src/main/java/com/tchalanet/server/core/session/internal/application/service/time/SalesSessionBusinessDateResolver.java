package com.tchalanet.server.core.session.internal.application.service.time;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Resolves the business date for a POS session open operation.
 *
 * <p>Priority:
 * <ol>
 *   <li>outlet timezone (outlet.timezone column)</li>
 *   <li>tenant timezone (TenantZoneApi)</li>
 *   <li>UTC fallback</li>
 * </ol>
 */
public interface SalesSessionBusinessDateResolver {
    LocalDate resolve(TenantId tenantId, OutletId outletId, Instant instant);
}
