package com.tchalanet.server.core.sales.application.command.model;

import com.tchalanet.server.common.bus.Command;

public record SyncOneOfflineSaleCommand(OfflineTicketSaleInput ticket)
    implements Command<SyncOfflineTicketDecision> {}

