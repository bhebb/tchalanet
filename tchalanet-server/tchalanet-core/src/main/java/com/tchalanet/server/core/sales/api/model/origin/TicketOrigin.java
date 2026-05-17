package com.tchalanet.server.core.sales.api.model.origin;

public record TicketOrigin(
    TicketSaleChannel channel,
    OfflineSaleRef offlineRef
) {
    public TicketOrigin {
        boolean isOffline = channel == TicketSaleChannel.POS_OFFLINE_SYNCED;
        boolean hasRef = offlineRef != null;
        if (isOffline != hasRef) {
            throw new IllegalArgumentException(
                "offlineRef must be present iff channel == POS_OFFLINE_SYNCED");
        }
    }
}
