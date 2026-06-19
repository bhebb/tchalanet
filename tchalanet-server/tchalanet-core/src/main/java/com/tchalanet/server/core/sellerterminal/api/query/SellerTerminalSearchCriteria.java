package com.tchalanet.server.core.sellerterminal.api.query;

import com.tchalanet.server.core.sellerterminal.api.model.SellerTerminalStatus;

/** All fields nullable = no filter applied. */
public record SellerTerminalSearchCriteria(
    String q,
    SellerTerminalStatus status
) {
    public static SellerTerminalSearchCriteria empty() {
        return new SellerTerminalSearchCriteria(null, null);
    }
}
