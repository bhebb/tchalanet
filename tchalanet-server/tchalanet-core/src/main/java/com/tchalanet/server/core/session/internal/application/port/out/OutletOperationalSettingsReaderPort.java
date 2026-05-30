package com.tchalanet.server.core.session.internal.application.port.out;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;

import java.time.ZoneId;
import java.util.Optional;

/** Reads outlet operational settings needed for session business logic. */
public interface OutletOperationalSettingsReaderPort {

    /**
     * Returns the outlet's configured timezone, or empty if not set.
     * The outlet timezone is authoritative for computing businessDate
     * (takes priority over the tenant-level timezone).
     */
    Optional<ZoneId> findOutletZone(TenantId tenantId, OutletId outletId);
}
