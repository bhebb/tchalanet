package com.tchalanet.server.core.sales.api.command;

import com.tchalanet.server.common.bus.Command;

public record SyncOneOfflineSaleCommand(OfflineTicketSaleInput ticket)
    implements Command<SyncOfflineTicketDecision> {}

