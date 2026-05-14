package com.tchalanet.server.core.sales.api.command;

import com.tchalanet.server.catalog.game.api.model.BetType;
import com.tchalanet.server.catalog.game.api.model.GameCode;
import java.math.BigDecimal;

public record SellTicketLineInput(
    GameCode gameCode,
    String selection,
    BetType betType,
    Short betOption,
    BigDecimal stakeAmount,
    BigDecimal oddsSnapshot
) {}

