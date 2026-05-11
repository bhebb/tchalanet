package com.tchalanet.server.core.sales.application.command.model.sell;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import java.math.BigDecimal;
import java.util.List;

public record SellTicketCommand(
    TenantId tenantId,
    UserId sellerUserId,
    TerminalId terminalId,
    OutletId outletId,
    SalesSessionId salesSessionId,
    DrawId drawId,
    String currency,
    BigDecimal feeAmount,
    List<SellTicketLineInput> lines
) implements Command<com.tchalanet.server.core.sales.application.command.model.sell.SellTicketResult> {}

