package com.tchalanet.server.core.sales.application.service;

import com.tchalanet.server.catalog.pricing.api.PricingCatalog;
import com.tchalanet.server.common.types.enums.BetType;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.sales.application.command.model.SellTicketCommand;
import com.tchalanet.server.core.sales.domain.model.TicketLine;
import com.tchalanet.server.core.sales.domain.service.BetSelectionNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class TicketLinePreparationService {

    private final BetSelectionNormalizer selectionNormalizer;
    private final PricingCatalog pricingCatalog;

    public List<SellTicketCommand.LineCommand> normalize(List<SellTicketCommand.LineCommand> lines) {
        return lines.stream()
            .map(l -> {
                validateOption(l.betType(), l.betOption());
                var normalizedSelection = selectionNormalizer.normalize(l.betType(), l.selection());
                return new SellTicketCommand.LineCommand(
                    l.gameCode(),
                    normalizedSelection,
                    requireStake(l.stake()),
                    l.betType(),
                    l.betOption()
                );
            })
            .toList();
    }

    public List<SellTicketCommand.LineCommand> mergeDuplicates(List<SellTicketCommand.LineCommand> lines) {
        record Key(com.tchalanet.server.common.types.enums.GameCode gameCode, String selection, BetType betType, Short betOption) {
        }

        Map<Key, BigDecimal> totals = new LinkedHashMap<>();
        for (var l : lines) {
            validateOption(l.betType(), l.betOption());
            totals.merge(
                new Key(l.gameCode(), l.selection(), l.betType(), l.betOption()),
                requireStake(l.stake()),
                BigDecimal::add
            );
        }

        return totals.entrySet().stream()
            .map(e -> new SellTicketCommand.LineCommand(
                e.getKey().gameCode(),
                e.getKey().selection(),
                e.getValue(),
                e.getKey().betType(),
                e.getKey().betOption()
            ))
            .toList();
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
        if (gameCode == null) throw new IllegalArgumentException("gameCode is required");
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
