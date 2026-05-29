package com.tchalanet.server.core.outlet.internal.domain.model;

import com.tchalanet.server.common.types.id.SalesZoneId;
import com.tchalanet.server.common.types.id.TenantId;
import java.time.Instant;

/** A commercial geographic zone grouping outlets. V1: code, label, parent, active. */
public record SalesZone(
    SalesZoneId id,
    TenantId tenantId,
    String code,
    String label,
    boolean active,
    SalesZoneId parentId,
    Instant createdAt,
    Instant updatedAt) {

    public static SalesZone createNew(
        SalesZoneId id,
        TenantId tenantId,
        String code,
        String label,
        SalesZoneId parentId,
        Instant now) {
        return new SalesZone(id, tenantId, code, label, true, parentId, now, now);
    }

    public SalesZone withLabel(String newLabel) {
        return new SalesZone(id, tenantId, code, newLabel, active, parentId, createdAt, updatedAt);
    }

    public SalesZone withActive(boolean newActive) {
        return new SalesZone(id, tenantId, code, label, newActive, parentId, createdAt, updatedAt);
    }

    public SalesZone withUpdatedAt(Instant now) {
        return new SalesZone(id, tenantId, code, label, active, parentId, createdAt, now);
    }
}
