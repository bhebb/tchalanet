package com.tchalanet.server.core.sales.api.model.print;

import com.tchalanet.server.catalog.game.api.model.GameCode;
import com.tchalanet.server.catalog.game.api.model.BetType;
import com.tchalanet.server.common.types.money.Money;

import java.math.BigDecimal;

public record TicketPrintLine(
    int lineNo,
    GameCode gameCode,
    BetType betType,
    Short betOption,
    String gameLabel,
    String selectionRaw,
    String selectionCanonical,
    BigDecimal odds,
    Money stake,
    Money potentialPayout
) {
}
