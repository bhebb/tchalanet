package com.tchalanet.server.core.session.internal.application.port.out;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.session.internal.domain.model.SalesSessionOpeningContext;

import java.time.LocalDate;

/**
 * Read-only optimized opening eligibility snapshot.
 *
 * <p>Replaces the previous dual-query pattern
 * ({@code findCurrentOpenByUser} + {@code existsForBusinessDate}) with a
 * single read that encapsulates the V1 constraint: only an OPEN session blocks
 * opening a new one. CLOSED sessions no longer block.
 */
public interface SalesSessionOpeningContextReaderPort {

    SalesSessionOpeningContext loadForOpening(
        TenantId tenantId,
        OutletId outletId,
        TerminalId terminalId,
        UserId openedBy,
        LocalDate businessDate
    );
}
