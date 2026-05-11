package com.tchalanet.server.core.sales.application.sell;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.context.OperationalRequestContext;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.types.money.CurrencyCode;
import com.tchalanet.server.core.sales.application.command.model.SellTicketResult;

import java.math.BigDecimal;

public record SellPosTicketCommand(
    TenantId tenantId,
    UserId sellerUserId,
    TerminalId terminalId,
    OutletId outletId,
    SalesSessionId salesSessionId,
    OperationalRequestContext operationalContext,
    DrawId drawId,
    CurrencyCode currency,
    BigDecimal feeAmount,
    List<SellTicketLineInput> lines
) implements Command<SellTicketResult> {}
