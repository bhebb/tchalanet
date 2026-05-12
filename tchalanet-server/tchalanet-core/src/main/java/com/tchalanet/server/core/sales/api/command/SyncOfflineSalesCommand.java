package com.tchalanet.server.core.sales.api.command;

import com.tchalanet.server.common.bus.Command;
import java.util.List;

public record SyncOfflineSalesCommand(List<OfflineTicketSaleInput> tickets)
    implements Command<SyncOfflineSalesResult> {}

