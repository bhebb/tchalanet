package com.tchalanet.server.core.sales.api.command.sell;

import com.tchalanet.server.catalog.game.api.model.BetType;
import com.tchalanet.server.catalog.game.api.model.GameCode;

import java.math.BigDecimal;

public record SellTicketLineInput(
    int lineNumber,
    GameCode gameCode,
    BetType betType,
    String rawSelection,
    Short betOption,
    BigDecimal stakeAmount
) {}
