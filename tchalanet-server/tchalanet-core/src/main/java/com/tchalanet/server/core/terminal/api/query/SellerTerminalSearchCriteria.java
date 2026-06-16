package com.tchalanet.server.core.terminal.api.query;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.core.terminal.api.model.SellerTerminalStatus;

/** All fields nullable = no filter applied. */
public record SellerTerminalSearchCriteria(
    String q,
    SellerTerminalStatus status,
    OutletId outletId
) {
    public static SellerTerminalSearchCriteria empty() {
        return new SellerTerminalSearchCriteria(null, null, null);
    }
}
