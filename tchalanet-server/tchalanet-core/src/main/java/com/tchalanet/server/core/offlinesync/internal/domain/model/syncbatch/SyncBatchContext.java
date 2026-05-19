package com.tchalanet.server.core.offlinesync.internal.domain.model.syncbatch;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;

import java.util.UUID;

/** Operational context (POS provenance) of a sync batch. */
public record SyncBatchContext(
    UUID deviceId,
    UserId sellerUserId,
    TerminalId terminalId,
    OutletId outletId,
    SalesSessionId salesSessionId
) {
    public SyncBatchContext {
        if (deviceId == null) throw new IllegalArgumentException("deviceId required");
        if (sellerUserId == null) throw new IllegalArgumentException("sellerUserId required");
        if (terminalId == null) throw new IllegalArgumentException("terminalId required");
        if (outletId == null) throw new IllegalArgumentException("outletId required");
        if (salesSessionId == null) throw new IllegalArgumentException("salesSessionId required");
    }
}
