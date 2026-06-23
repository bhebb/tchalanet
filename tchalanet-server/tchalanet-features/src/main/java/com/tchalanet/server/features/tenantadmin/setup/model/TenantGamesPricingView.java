package com.tchalanet.server.features.tenantadmin.setup.model;

import com.tchalanet.server.common.types.id.TenantGameId;
import com.tchalanet.server.core.limitpolicy.api.RuleKey;
import com.tchalanet.server.core.limitpolicy.BreachOutcome;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.util.List;

public record TenantGamesPricingView(
    List<GamePricingRow> games
) {

    public record GamePricingRow(
        String gameCode,
        TenantGameId tenantGameId,
        String catalogName,
        String category,
        String displayName,
        int displayOrder,
        boolean enabled,
        boolean visibleInPos,
        BigDecimal minStake,
        BigDecimal maxStake,
        LimitsView limits,
        PricingView pricing
    ) {}

    public record LimitsView(
        boolean configured,
        List<LimitAssignmentRow> assignments
    ) {}

    public record LimitAssignmentRow(
        RuleKey ruleKey,
        boolean enabled,
        BreachOutcome onBreach,
        JsonNode params
    ) {}

    public record PricingView(
        boolean configured,
        List<PricingEntryRow> entries
    ) {}

    public record PricingEntryRow(
        String betType,
        Short betOption,
        BigDecimal odds
    ) {}
}
