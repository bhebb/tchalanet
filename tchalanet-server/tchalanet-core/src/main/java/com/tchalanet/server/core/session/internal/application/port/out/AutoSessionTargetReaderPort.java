package com.tchalanet.server.core.session.internal.application.port.out;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.session.internal.domain.model.AutoSessionCloseTarget;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public interface AutoSessionTargetReaderPort {

    /**
     * Returns all OPEN sessions for a tenant whose businessDate is strictly before
     * {@code cutoffDate}. Used by the nightly auto-close scheduler to close forgotten
     * sessions from previous business days without touching the current business day.
     *
     * <p>Must be called within a bound tenant RLS context.
     */
    List<AutoSessionCloseTarget> findOpenSessionsBeforeBusinessDate(
        TenantId tenantId,
        LocalDate cutoffDate,
        Instant closedAt,
        String reason);

    List<AutoSessionCloseTarget> findOpenCloseTargetsByOutlet(
        TenantId tenantId,
        OutletId outletId,
        Instant closedAt,
        UserId closedBy,
        String reason);
}
