package com.tchalanet.server.core.sales.application.command.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.core.sales.domain.model.BetType;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Command to sell a ticket (purchase).
 */
public record SellTicketCommand(
    UUID tenantId,
    UUID terminalId,
    UUID cashierId,
    UUID drawId,
    List<LineCommand> lines,
    String currency
) implements Command<SellTicketResult> {
    public record LineCommand(String gameCode, String selection, BigDecimal stake, BetType betType) {}
}
