package com.tchalanet.server.features.tenantadmin.setup.model;

import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.ResultSlotId;
import com.tchalanet.server.common.types.id.TenantGameId;
import com.tchalanet.server.core.limitpolicy.api.RuleKey;
import com.tchalanet.server.core.limitpolicy.BreachOutcome;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

public record TenantDrawSalesMatrixView(
    MatrixSummary summary,
    List<ProviderMatrixView> providers
) {

    public record MatrixSummary(
        int providerCount,
        int slotCount,
        int configuredChannelCount,
        int activeChannelCount,
        int supportedTenantGameCount,
        int offeredChannelGameCount,
        int saleReadyChannelGameCount,
        int missingStakeConfigCount,
        int missingLimitCount
    ) {}

    public record ProviderMatrixView(
        String providerCode,
        List<SlotMatrixView> slots
    ) {}

    public record SlotMatrixView(
        String slotKey,
        String labelKey,
        ResultSlotSetupView resultSlot,
        DrawChannelSetupView channel,
        List<ChannelGameSetupView> games,
        boolean slotReady,
        List<SetupWarning> warnings
    ) {}

    public record ResultSlotSetupView(
        ResultSlotId resultSlotId,
        LocalTime drawTime,
        String daysOfWeek,
        boolean active,
        JsonNode sourceCfg,
        JsonNode projectionCfg
    ) {}

    public record DrawChannelSetupView(
        DrawChannelId drawChannelId,
        String channelCode,
        boolean active,
        boolean configured,
        LocalTime drawTime,
        LocalTime salesOpenTime,
        int cutoffSec,
        String defaultSource,
        int sortOrder,
        DrawChannelId dependsOnChannelId
    ) {}

    public record ChannelGameSetupView(
        String gameCode,
        TenantGameId tenantGameId,
        String displayName,
        boolean enabledForTenant,
        boolean visibleInPos,
        boolean offeredOnChannel,
        boolean enabledOnChannel,
        BigDecimal minStake,
        BigDecimal maxStake,
        LimitsSetupView limits,
        boolean saleReady,
        List<SetupWarning> warnings
    ) {}

    public record LimitsSetupView(
        boolean configured,
        List<LimitAssignmentRow> assignments
    ) {}

    public record LimitAssignmentRow(
        RuleKey ruleKey,
        boolean enabled,
        BreachOutcome onBreach,
        JsonNode params
    ) {}

    public record SetupWarning(String code, String severity) {}
}
