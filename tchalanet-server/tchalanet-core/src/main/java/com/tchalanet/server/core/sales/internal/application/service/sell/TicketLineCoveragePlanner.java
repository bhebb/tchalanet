package com.tchalanet.server.core.sales.internal.application.service.sell;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.pricing.api.model.PricingVariantCode;
import com.tchalanet.server.core.sales.api.command.sell.SellTicketLineInput;
import com.tchalanet.server.core.sales.api.model.coverage.PotentialGainMode;
import com.tchalanet.server.core.sales.internal.domain.model.ticket.WinMode;
import com.tchalanet.server.core.sales.internal.domain.service.result.SettlementVariantResolver;
import com.tchalanet.server.platform.tenantgame.api.TenantGameApi;
import com.tchalanet.server.platform.tenantgame.api.model.SelectionPolicy;
import com.tchalanet.server.platform.tenantgame.api.model.view.TenantBetOptionView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;

@Component
@RequiredArgsConstructor
class TicketLineCoveragePlanner {

    private final TenantGameApi tenantGameApi;

    TicketLineCoveragePlan plan(TenantId tenantId, SellTicketLineInput input) {
        if (!input.betType().requiresOption()) {
            var resolution = SettlementVariantResolver.resolveCoverage(
                input.betType(),
                input.betOption(),
                input.rawSelection());
            return new TicketLineCoveragePlan(
                resolution.potentialGainMode(),
                StakeAllocationMode.FULL_STAKE_PER_ALTERNATIVE,
                input.betOption(),
                SelectionPolicy.EXPLICIT_ONLY,
                null,
                resolution.variants().stream()
                    .map(variant -> new PlannedTicketLineCoverage(
                        input.betOption(),
                        variant.pricingVariantCode(),
                        variant.winMode()))
                    .toList());
        }

        var betTypeConfig = tenantGameApi.getBetOptionConfig(tenantId, input.gameCode().name())
            .betTypes().stream()
            .filter(config -> config.betType() == input.betType())
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("tenant bet type config is required"));

        if (betTypeConfig.selectionPolicy() != SelectionPolicy.IMPLICIT_BEST_MATCH) {
            var resolution = SettlementVariantResolver.resolveCoverage(
                input.betType(),
                input.betOption(),
                input.rawSelection());
            var selectedOptionLabel = betTypeConfig.options().stream()
                .filter(option -> java.util.Objects.equals(option.code(), input.betOption()))
                .map(TenantBetOptionView::label)
                .findFirst()
                .orElse(null);
            return new TicketLineCoveragePlan(
                resolution.potentialGainMode(),
                resolution.variants().size() == 1
                    ? StakeAllocationMode.FULL_STAKE_PER_ALTERNATIVE
                    : StakeAllocationMode.SPLIT_ACROSS_COVERAGES,
                input.betOption(),
                betTypeConfig.selectionPolicy(),
                selectedOptionLabel,
                resolution.variants().stream()
                    .map(variant -> new PlannedTicketLineCoverage(
                        input.betOption(),
                        variant.pricingVariantCode(),
                        variant.winMode()))
                    .toList());
        }

        var plannedByVariant = new LinkedHashMap<CoverageKey, PlannedTicketLineCoverage>();
        betTypeConfig.options().stream()
            .filter(option -> option.enabled())
            .sorted(Comparator.comparingInt(option -> option.displayOrder()))
            .forEach(option -> {
                var resolution = SettlementVariantResolver.resolveCoverage(
                    input.betType(),
                    option.code(),
                    input.rawSelection());
                resolution.variants().forEach(variant -> {
                    var key = new CoverageKey(variant.pricingVariantCode(), variant.winMode());
                    plannedByVariant.putIfAbsent(key, new PlannedTicketLineCoverage(
                        option.code(),
                        variant.pricingVariantCode(),
                        variant.winMode()));
                });
            });

        if (plannedByVariant.isEmpty()) {
            throw new IllegalStateException("implicit tenant coverage plan is empty");
        }

        var planned = List.copyOf(plannedByVariant.values());
        return new TicketLineCoveragePlan(
            planned.size() == 1 ? PotentialGainMode.SINGLE : PotentialGainMode.RANGE_ALTERNATIVE,
            StakeAllocationMode.FULL_STAKE_PER_ALTERNATIVE,
            planned.getFirst().sourceBetOption(),
            betTypeConfig.selectionPolicy(),
            null,
            planned);
    }

    private record CoverageKey(
        PricingVariantCode pricingVariantCode,
        WinMode winMode
    ) {}
}

record TicketLineCoveragePlan(
    PotentialGainMode potentialGainMode,
    StakeAllocationMode stakeAllocationMode,
    Short canonicalBetOption,
    SelectionPolicy selectionPolicySnapshot,
    String betOptionLabelSnapshot,
    List<PlannedTicketLineCoverage> coverages
) {}

record PlannedTicketLineCoverage(
    Short sourceBetOption,
    PricingVariantCode pricingVariantCode,
    WinMode winMode
) {}

enum StakeAllocationMode {
    FULL_STAKE_PER_ALTERNATIVE,
    SPLIT_ACROSS_COVERAGES
}
