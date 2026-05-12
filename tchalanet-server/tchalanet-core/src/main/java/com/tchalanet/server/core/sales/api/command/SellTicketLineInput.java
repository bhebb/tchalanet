package com.tchalanet.server.core.sales.api.command;

import com.tchalanet.server.common.types.enums.BetType;
import com.tchalanet.server.common.types.enums.GameCode;
import java.math.BigDecimal;

public record SellTicketLineInput(
    GameCode gameCode,
    String selection,
    BetType betType,
    Short betOption,
    BigDecimal stakeAmount,
    BigDecimal oddsSnapshot
) {}

