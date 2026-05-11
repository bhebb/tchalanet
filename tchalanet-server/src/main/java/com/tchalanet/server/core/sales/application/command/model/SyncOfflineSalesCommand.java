package com.tchalanet.server.core.sales.application.command.model;

import com.tchalanet.server.common.bus.Command;
import java.util.List;

public record SyncOfflineSalesCommand(List<OfflineTicketSaleInput> tickets)
    implements Command<SyncOfflineSalesResult> {}

