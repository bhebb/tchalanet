package com.tchalanet.server.features.tenantadmin.setup;

import com.tchalanet.server.catalog.drawchannel.api.DrawChannelCatalog;
import com.tchalanet.server.catalog.drawchannel.api.model.DrawChannelCalendarRow;
import com.tchalanet.server.catalog.resultslot.api.ResultSlotCatalog;
import com.tchalanet.server.catalog.resultslot.api.ResultSlotView;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.ResultSlotId;
import com.tchalanet.server.common.types.id.TenantGameId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.limitpolicy.api.query.ListLimitAssignmentsByScopeQuery;
import com.tchalanet.server.core.limitpolicy.api.query.ListLimitAssignmentsView;
import com.tchalanet.server.core.limitpolicy.api.query.LimitScopeQueryRef;
import com.tchalanet.server.features.tenantadmin.setup.model.TenantDrawSalesMatrixView;
import com.tchalanet.server.features.tenantadmin.setup.model.TenantDrawSalesMatrixView.ChannelGameSetupView;
import com.tchalanet.server.features.tenantadmin.setup.model.TenantDrawSalesMatrixView.DrawChannelSetupView;
import com.tchalanet.server.features.tenantadmin.setup.model.TenantDrawSalesMatrixView.LimitAssignmentRow;
import com.tchalanet.server.features.tenantadmin.setup.model.TenantDrawSalesMatrixView.LimitsSetupView;
import com.tchalanet.server.features.tenantadmin.setup.model.TenantDrawSalesMatrixView.MatrixSummary;
import com.tchalanet.server.features.tenantadmin.setup.model.TenantDrawSalesMatrixView.ProviderMatrixView;
import com.tchalanet.server.features.tenantadmin.setup.model.TenantDrawSalesMatrixView.ResultSlotSetupView;
import com.tchalanet.server.features.tenantadmin.setup.model.TenantDrawSalesMatrixView.SetupWarning;
import com.tchalanet.server.features.tenantadmin.setup.model.TenantDrawSalesMatrixView.SlotMatrixView;
import com.tchalanet.server.platform.tenantgame.api.TenantGameApi;
import com.tchalanet.server.platform.tenantgame.api.model.view.TenantGameRefView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TenantDrawSalesMatrixService {

    private final ResultSlotCatalog resultSlotCatalog;
    private final DrawChannelCatalog drawChannelCatalog;
    private final TenantGameApi tenantGameApi;
    private final QueryBus queryBus;

    public TenantDrawSalesMatrixView get(TenantId tenantId) {
        var resultSlots = resultSlotCatalog.listActive();
        var calendarRows = drawChannelCatalog.listCalendarRows(tenantId, null, null);
        var tenantGames = tenantGameApi.listGames(tenantId).stream()
            .sorted(Comparator.comparingInt(TenantGameRefView::displayOrder))
            .toList();
        var limitAssignments = queryBus.ask(
            new ListLimitAssignmentsByScopeQuery(LimitScopeQueryRef.tenant(tenantId)));

        // Build indexes
        // One representative calendar row per channelId (channel-level fields, not game-level)
        Map<DrawChannelId, DrawChannelCalendarRow> channelRepresentative = new LinkedHashMap<>();
        // channelId → resultSlotId mapping (from any row for that channel)
        Map<DrawChannelId, ResultSlotId> channelResultSlot = new LinkedHashMap<>();
        // resultSlotId → channelId (first channel configured for that slot)
        Map<ResultSlotId, DrawChannelId> slotToChannel = new LinkedHashMap<>();
        // channelId × tenantGameId → row (offered games)
        Map<DrawChannelId, Map<TenantGameId, DrawChannelCalendarRow>> offeredGames = new LinkedHashMap<>();

        for (var row : calendarRows) {
            channelRepresentative.putIfAbsent(row.channelId(), row);
            channelResultSlot.putIfAbsent(row.channelId(), row.resultSlotId());
            slotToChannel.putIfAbsent(row.resultSlotId(), row.channelId());
            offeredGames.computeIfAbsent(row.channelId(), k -> new LinkedHashMap<>())
                .put(row.tenantGameId(), row);
        }

        // tenantGameId index
        Map<TenantGameId, TenantGameRefView> tenantGameById = tenantGames.stream()
            .collect(Collectors.toMap(TenantGameRefView::tenantGameId, tg -> tg));

        // Limit assignments (tenant-scoped)
        var limitRows = toLimitRows(limitAssignments);
        var limitsSetup = new LimitsSetupView(!limitRows.isEmpty(), limitRows);

        // Group result slots by provider
        Map<String, List<ResultSlotView>> slotsByProvider = resultSlots.stream()
            .collect(Collectors.groupingBy(
                s -> s.provider() != null ? s.provider() : "UNKNOWN",
                LinkedHashMap::new,
                Collectors.toList()));

        // Counters for summary
        int configuredChannelCount = 0;
        int activeChannelCount = 0;
        int offeredChannelGameCount = 0;
        int saleReadyChannelGameCount = 0;
        int missingStakeConfigCount = 0;
        int missingLimitCount = 0;

        List<ProviderMatrixView> providers = new ArrayList<>();

        for (var entry : slotsByProvider.entrySet()) {
            String provider = entry.getKey();
            List<SlotMatrixView> slotViews = new ArrayList<>();

            for (var slot : entry.getValue()) {
                DrawChannelId channelId = slotToChannel.get(slot.id());
                DrawChannelCalendarRow rep = channelId != null ? channelRepresentative.get(channelId) : null;
                boolean channelConfigured = rep != null;

                if (channelConfigured) {
                    configuredChannelCount++;
                    if (rep.active()) activeChannelCount++;
                }

                DrawChannelSetupView channelSetup = channelConfigured
                    ? toChannelSetup(rep, channelId) : null;

                // Build game rows: all tenant games for this channel
                List<ChannelGameSetupView> gameViews = new ArrayList<>();
                Map<TenantGameId, DrawChannelCalendarRow> offered =
                    channelId != null ? offeredGames.getOrDefault(channelId, Map.of()) : Map.of();

                for (TenantGameRefView tg : tenantGames) {
                    DrawChannelCalendarRow dcgRow = offered.get(tg.tenantGameId());
                    boolean offeredOnChannel = dcgRow != null;
                    boolean enabledOnChannel = offeredOnChannel && dcgRow.enabled();

                    List<SetupWarning> gameWarnings = new ArrayList<>();
                    if (!slot.active()) gameWarnings.add(new SetupWarning("resultslot.inactive", "ERROR"));
                    if (!channelConfigured) gameWarnings.add(new SetupWarning("draw_channel.not_configured", "ERROR"));
                    else if (!rep.active()) gameWarnings.add(new SetupWarning("draw_channel.inactive", "WARN"));
                    if (!tg.enabled()) gameWarnings.add(new SetupWarning("tenant_game.disabled", "WARN"));
                    if (!tg.visibleInPos()) gameWarnings.add(new SetupWarning("tenant_game.hidden_in_pos", "WARN"));
                    if (!offeredOnChannel) gameWarnings.add(new SetupWarning("channel_game.not_offered", "INFO"));
                    else if (!enabledOnChannel) gameWarnings.add(new SetupWarning("channel_game.disabled", "WARN"));
                    if (tg.minStake() == null || tg.maxStake() == null) gameWarnings.add(new SetupWarning("stake_config.missing", "WARN"));
                    if (!limitsSetup.configured()) gameWarnings.add(new SetupWarning("limitpolicy.missing", "WARN"));

                    boolean saleReady = slot.active()
                        && channelConfigured && rep.active()
                        && tg.enabled() && tg.visibleInPos()
                        && offeredOnChannel && enabledOnChannel
                        && tg.minStake() != null && tg.maxStake() != null
                        && limitsSetup.configured();

                    if (offeredOnChannel) offeredChannelGameCount++;
                    if (saleReady) saleReadyChannelGameCount++;
                    if (offeredOnChannel && (tg.minStake() == null || tg.maxStake() == null)) missingStakeConfigCount++;
                    if (offeredOnChannel && !limitsSetup.configured()) missingLimitCount++;

                    gameViews.add(new ChannelGameSetupView(
                        tg.gameCode(), tg.tenantGameId(), tg.displayName(),
                        tg.enabled(), tg.visibleInPos(),
                        offeredOnChannel, enabledOnChannel,
                        tg.minStake(), tg.maxStake(),
                        limitsSetup, saleReady, gameWarnings));
                }

                boolean slotReady = slot.active() && channelConfigured && rep != null && rep.active()
                    && gameViews.stream().anyMatch(ChannelGameSetupView::saleReady);

                List<SetupWarning> slotWarnings = new ArrayList<>();
                if (!slot.active()) slotWarnings.add(new SetupWarning("resultslot.inactive", "ERROR"));
                if (!channelConfigured) slotWarnings.add(new SetupWarning("draw_channel.not_configured", "ERROR"));
                else if (rep != null && !rep.active()) slotWarnings.add(new SetupWarning("draw_channel.inactive", "WARN"));

                slotViews.add(new SlotMatrixView(
                    slot.slotKey(), slot.labelKey(),
                    toResultSlotSetup(slot),
                    channelSetup,
                    gameViews,
                    slotReady,
                    slotWarnings));
            }

            providers.add(new ProviderMatrixView(provider, slotViews));
        }

        var summary = new MatrixSummary(
            slotsByProvider.size(),
            resultSlots.size(),
            configuredChannelCount,
            activeChannelCount,
            tenantGames.size(),
            offeredChannelGameCount,
            saleReadyChannelGameCount,
            missingStakeConfigCount,
            missingLimitCount);

        return new TenantDrawSalesMatrixView(summary, providers);
    }

    private DrawChannelSetupView toChannelSetup(DrawChannelCalendarRow rep, DrawChannelId channelId) {
        return new DrawChannelSetupView(
            channelId,
            rep.code(),
            rep.active(),
            true,
            rep.drawTime(),
            rep.salesOpenTime(),
            rep.cutoffSec(),
            rep.defaultSource(),
            rep.sortOrder(),
            rep.dependsOnChannelId());
    }

    private ResultSlotSetupView toResultSlotSetup(ResultSlotView slot) {
        return new ResultSlotSetupView(
            slot.id(), slot.drawTime(), slot.daysOfWeek(),
            slot.active(), slot.sourceCfg(), slot.projectionCfg());
    }

    private List<LimitAssignmentRow> toLimitRows(ListLimitAssignmentsView assignments) {
        return assignments.items().stream()
            .filter(ListLimitAssignmentsView.Item::enabled)
            .map(item -> new LimitAssignmentRow(
                item.ruleKey(), item.enabled(), item.onBreach(), item.params()))
            .toList();
    }
}
