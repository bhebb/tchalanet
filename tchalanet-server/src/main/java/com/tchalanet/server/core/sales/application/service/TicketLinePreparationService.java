package com.tchalanet.server.core.sales.application.service;

import com.tchalanet.server.common.types.enums.BetType;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.catalog.pricing.api.PricingCatalog;
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
            .map(
                l ->
                    new SellTicketCommand.LineCommand(
                        l.gameCode(),
                        selectionNormalizer.normalize(l.betType(), l.selection()),
                        l.stake(),
                        l.betType(),
                        l.betOption() // keep option
                    ))
            .toList();
    }

    /**
     * Merge duplicate (gameCode + betType + betOption + selection) by summing stakes.
     */
    public List<SellTicketCommand.LineCommand> mergeDuplicates(List<SellTicketCommand.LineCommand> lines) {
        record Key(String gameCode, String selection, BetType betType, Short betOption) {}

        Map<Key, BigDecimal> totals = new LinkedHashMap<>();
        for (var l : lines) {
            if (l.stake() == null || l.stake().signum() <= 0) {
                throw new IllegalArgumentException("Stake must be > 0");
            }
            totals.merge(
                new Key(l.gameCode(), l.selection(), l.betType(), l.betOption()),
                l.stake(),
                BigDecimal::add);
        }

        return totals.entrySet().stream()
            .map(
                e ->
                    new SellTicketCommand.LineCommand(
                        e.getKey().gameCode(),
                        e.getKey().selection(),
                        e.getValue(),
                        e.getKey().betType(),
                        e.getKey().betOption()))
            .toList();
    }

    /**
     * Pricing snapshot: odds depend on betType + betOption (for pattern types).
     */
    public List<TicketLine> toTicketLines(TenantId tenantId, List<SellTicketCommand.LineCommand> lines) {
        return lines.stream()
            .map(l -> {
                BigDecimal stake = l.stake().setScale(2, RoundingMode.UNNECESSARY);

                BigDecimal odds = pricingCatalog
                    .oddsFor(tenantId, l.gameCode(), l.betType(), l.betOption())
                    .setScale(4, RoundingMode.HALF_UP);

                BigDecimal potential = stake.multiply(odds).setScale(2, RoundingMode.HALF_UP);

                return new TicketLine(
                    l.gameCode(),
                    l.selection(),
                    stake,
                    odds,
                    potential,
                    l.betType(),
                    l.betOption() // ideally Short later
                );
            })
            .toList();
    }

}
