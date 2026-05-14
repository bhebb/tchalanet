package com.tchalanet.server.core.sales.api.print;

import com.tchalanet.server.catalog.game.api.model.BetType;
import java.math.BigDecimal;

public record TicketPrintLine(
    String gameCode,
    BetType betType,
    Short betOption,
    String selection,
    BigDecimal stake,
    BigDecimal potentialPayout
) {}
