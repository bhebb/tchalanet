package com.tchalanet.server.core.sales.internal.domain.model.ticket;

import com.tchalanet.server.catalog.game.api.model.BetType;
import com.tchalanet.server.catalog.game.api.model.GameCode;
import com.tchalanet.server.common.types.id.TicketLineId;
import com.tchalanet.server.common.types.money.Money;
import com.tchalanet.server.core.sales.api.model.line.TicketLineResult;
import com.tchalanet.server.core.sales.api.model.status.TicketLineResultStatus;
import com.tchalanet.server.core.selection.api.model.Selection;

import java.math.BigDecimal;

public record TicketLine(
    TicketLineId id,
    int lineNumber,
    GameCode gameCode,
    BetType betType,
    Selection selection,
    Money stakeAmount,
    BigDecimal oddsSnapshot,
    Money potentialPayoutAmount,
    Short betOption,
    TicketLineResultStatus resultStatus,
    Money payoutAmount
) {
    public TicketLine {
        if (!stakeAmount.currency().equals(potentialPayoutAmount.currency())) {
            throw new IllegalArgumentException("Stake/potentialPayout currency mismatch");
        }
        if (!stakeAmount.currency().equals(payoutAmount.currency())) {
            throw new IllegalArgumentException("Stake/payout currency mismatch");
        }
        if (resultStatus == TicketLineResultStatus.PENDING && !payoutAmount.isZero()) {
            throw new IllegalArgumentException("Pending line must have zero payout");
        }
        if (lineNumber < 1) {
            throw new IllegalArgumentException("Line number must be >= 1");
        }
    }

    public TicketLine withResult(TicketLineResult result) {
        return new TicketLine(
            id, lineNumber, gameCode, betType, selection, stakeAmount, oddsSnapshot,
            potentialPayoutAmount, betOption,
            result.status(),
            result.payoutAmount()
        );
    }
}
