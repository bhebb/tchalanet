package com.tchalanet.server.core.sales.internal.application.service;

import com.tchalanet.server.catalog.pricing.api.PricingCatalog;
import com.tchalanet.server.common.types.enums.BetType;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.sales.application.command.model.SellTicketCommand;
import com.tchalanet.server.core.sales.application.command.model.SellTicketLineInput;
import com.tchalanet.server.core.sales.domain.model.TicketLine;
import com.tchalanet.server.core.sales.domain.service.BetSelectionNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class TicketLinePreparationService {
    private final BetSelectionNormalizer selectionNormalizer;
    private final PricingCatalog pricingCatalog;

    public List<TicketLine> prepare(List<SellTicketLineInput> inputs) {
        List<TicketLine> out = new ArrayList<>();
        int lineNo = 1;
        for (var in : inputs) {
            BigDecimal potential = in.stakeAmount().multiply(in.oddsSnapshot());
            out.add(new TicketLine(
                lineNo++,
                in.gameCode(),
                in.selection(),
                in.betType(),
                in.betOption(),
                in.stakeAmount(),
                in.oddsSnapshot(),
                potential));
        }
        return out;
    }

    public List<TicketLine> toTicketLines(TenantId tenantId, List<SellTicketCommand.LineCommand> lines) {
        return lines.stream()
            .map(l -> {
                validateOption(l.betType(), l.betOption());

                BigDecimal stake = requireStake(l.stake()).setScale(2, RoundingMode.UNNECESSARY);

                BigDecimal odds = pricingCatalog
                    .oddsFor(tenantId, canonicalGameCode(l.gameCode()), l.betType(), l.betOption())
                    .setScale(4, RoundingMode.HALF_UP); // or UNNECESSARY

                BigDecimal potential = stake.multiply(odds).setScale(2, RoundingMode.HALF_UP);

                return new TicketLine(
                    l.gameCode(),
                    l.selection(),
                    stake,
                    odds,
                    potential,
                    l.betType(),
                    l.betOption()
                );
            })
            .toList();
    }

    private static BigDecimal requireStake(BigDecimal stake) {
        if (stake == null || stake.signum() <= 0) {
            throw new IllegalArgumentException("Stake must be > 0");
        }
        return stake;
    }

    // With GameCode enum in web/domain, canonicalization is no-op: return enum name
    private static String canonicalGameCode(com.tchalanet.server.common.types.enums.GameCode gameCode) {
        if (gameCode == null) throw new IllegalArgumentException("externalGameCode is required");
        return gameCode.name();
    }

    private static void validateOption(BetType betType, Short betOption) {
        if (betType == null) throw new IllegalArgumentException("betType is required");

        if (betType.requiresBetOption()) {
            if (betOption == null) throw new IllegalArgumentException("betOption is required for " + betType);
            if (betOption < betType.betOptionMin() || betOption > betType.betOptionMax()) {
                throw new IllegalArgumentException("betOption out of range for " + betType);
            }
        } else {
            if (betOption != null) throw new IllegalArgumentException("betOption must be null for " + betType);
        }
    }
}

