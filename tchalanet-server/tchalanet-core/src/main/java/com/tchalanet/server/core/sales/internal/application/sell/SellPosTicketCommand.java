package com.tchalanet.server.core.sales.internal.application.sell;

import com.tchalanet.server.common.context.OperationalRequestContext;
import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.types.money.CurrencyCode;
import com.tchalanet.server.core.sales.api.command.SellTicketLineInput;
import com.tchalanet.server.core.sales.api.command.SellTicketResult;

import java.math.BigDecimal;
import java.util.List;

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
