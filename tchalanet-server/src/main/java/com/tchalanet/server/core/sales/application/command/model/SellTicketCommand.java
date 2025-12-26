package com.tchalanet.server.core.sales.application.command.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.enums.BetType;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import java.math.BigDecimal;
import java.util.List;

/** Command to sell a ticket (purchase). */
public record SellTicketCommand(
    TenantId tenantId,
    TerminalId terminalId,
    UserId cashierId,
    DrawId drawId,
    List<LineCommand> lines,
    String currency)
    implements Command<SellTicketResult> {
  public record LineCommand(String gameCode, String selection, BigDecimal stake, BetType betType) {}
}
