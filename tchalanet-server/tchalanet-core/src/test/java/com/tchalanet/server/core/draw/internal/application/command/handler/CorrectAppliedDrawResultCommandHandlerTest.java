package com.tchalanet.server.core.draw.internal.application.command.handler;

import com.tchalanet.server.catalog.drawchannel.api.DrawChannelCatalog;
import com.tchalanet.server.catalog.drawchannel.api.model.DrawChannelView;
import com.tchalanet.server.catalog.drawchannel.api.model.DrawSource;
import com.tchalanet.server.common.event.DomainEvent;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.DrawResultId;
import com.tchalanet.server.common.types.id.ResultSlotId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.draw.api.command.CorrectAppliedDrawResultCommand;
import com.tchalanet.server.core.draw.api.event.DrawResultCorrectedEvent;
import com.tchalanet.server.core.draw.internal.application.port.out.DrawLifecyclePort;
import com.tchalanet.server.core.draw.internal.application.port.out.DrawLookupPort;
import com.tchalanet.server.core.draw.internal.application.port.out.DrawSalesGuardPort;
import com.tchalanet.server.core.draw.internal.application.query.projection.DueToCloseRow;
import com.tchalanet.server.core.draw.internal.application.query.projection.ExistingDrawKey;
import com.tchalanet.server.core.draw.internal.application.query.projection.NewDrawRow;
import com.tchalanet.server.core.draw.internal.application.query.projection.OpenableDrawRow;
import com.tchalanet.server.core.draw.internal.domain.model.Draw;
import com.tchalanet.server.core.draw.internal.domain.model.DrawStatus;
import com.tchalanet.server.platform.idempotence.api.ProcessedEventPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CorrectAppliedDrawResultCommandHandler")
class CorrectAppliedDrawResultCommandHandlerTest {

    private final TenantId tenantId = TenantId.of(UUID.randomUUID());
    private final DrawId drawId = DrawId.of(UUID.randomUUID());
    private final DrawChannelId drawChannelId = DrawChannelId.of(UUID.randomUUID());
    private final DrawResultId previousResultId = DrawResultId.of(UUID.randomUUID());
    private final DrawResultId correctedResultId = DrawResultId.of(UUID.randomUUID());
    private final ResultSlotId resultSlotId = ResultSlotId.of(UUID.randomUUID());

    private Draw draw;
    private final List<DomainEvent> published = new ArrayList<>();
    private int saveCalls = 0;
    private final Set<String> processed = new HashSet<>();

    private CorrectAppliedDrawResultCommandHandler handler;

    @BeforeEach
    void setUp() {
        draw = resultedDraw(previousResultId);

        DrawLookupPort drawLookupPort = new DrawLookupPort() {
            @Override public Optional<Draw> findById(DrawId drawId) { return Optional.of(draw); }
            @Override public Draw getById(DrawId drawId) { return draw; }
            @Override public Draw getByIdForUpdate(DrawId drawId) { return draw; }
            @Override public boolean existsSettledDrawForResult(DrawResultId drawResultId) { return false; }
        };

        DrawLifecyclePort drawLifecyclePort = new DrawLifecyclePort() {
            @Override public List<OpenableDrawRow> findOpenable(Instant now, int limit, int openHorizonHours, int openLagHours) { return List.of(); }
            @Override public List<OpenableDrawRow> findOpenableForSalesOpenTime(Instant now, LocalDate drawDate, LocalTime defaultSalesOpenTime, int limit) { return List.of(); }
            @Override public int bulkOpen(List<DrawId> drawIds, Instant now) { return 0; }
            @Override public List<DueToCloseRow> findDueToClose(Instant now, int limit) { return List.of(); }
            @Override public int bulkClose(List<DrawId> drawIds, Instant now) { return 0; }
            @Override public int bulkInsert(List<NewDrawRow> rows) { return 0; }
            @Override public Set<ExistingDrawKey> findExistingKeys(TenantId tenantId, LocalDate from, LocalDate to) { return Set.of(); }
            @Override public Draw save(Draw draw) { saveCalls++; return draw; }
        };

        DrawSalesGuardPort salesGuard = new DrawSalesGuardPort() {
            @Override public void assertCanCancel(DrawId drawId, boolean force) { }
            @Override public void assertCanArchive(DrawId drawId, boolean force) { }
            @Override public void assertCanCorrectAppliedResult(DrawId drawId, DrawResultId correctedDrawResultId, boolean force) { }
        };

        DrawChannelCatalog drawChannelCatalog = new DrawChannelCatalog() {
            @Override public List<com.tchalanet.server.catalog.drawchannel.api.model.DrawChannelSummaryView> listAll(TenantId tenantId, Boolean activeOnly) { return List.of(); }
            @Override public Optional<DrawChannelView> findById(TenantId tenantId, DrawChannelId id) {
                return Optional.of(new DrawChannelView(
                    drawChannelId, tenantId, "CH", "Channel", "Channel", ZoneId.of("UTC"),
                    LocalTime.NOON, 60, List.of(DayOfWeek.MONDAY), true, 1,
                    "DAILY", null, null, resultSlotId, DrawSource.SYSTEM, Instant.now(), Instant.now()));
            }
            @Override public Optional<DrawChannelView> findByTenantAndCode(TenantId tenantId, String code) { return Optional.empty(); }
            @Override public List<com.tchalanet.server.catalog.drawchannel.api.model.DrawChannelGameView> listGamesByChannel(TenantId tenantId, DrawChannelId channelId) { return List.of(); }
            @Override public List<com.tchalanet.server.catalog.drawchannel.api.model.ChannelGamesView> listChannelGames(TenantId tenantId) { return List.of(); }
            @Override public List<com.tchalanet.server.catalog.drawchannel.api.model.DrawChannelCalendarRow> listCalendarRows(TenantId tenantId, Boolean activeOnly, Boolean enabledOnly) { return List.of(); }
            @Override public com.tchalanet.server.common.web.paging.TchPage<DrawChannelView> search(com.tchalanet.server.catalog.drawchannel.api.model.DrawChannelSearchCriteria criteria, com.tchalanet.server.common.web.paging.TchPageRequest pageReq) { return com.tchalanet.server.common.web.paging.TchPage.of(List.of(), 0, 20, 0, 0, true, false, false); }
        };

        ProcessedEventPort processedEventPort = new ProcessedEventPort() {
            @Override public boolean alreadyProcessed(String handlerKey, UUID eventId) { return processed.contains(handlerKey + ":" + eventId); }
            @Override public void markProcessed(String handlerKey, UUID eventId) { processed.add(handlerKey + ":" + eventId); }
            @Override public boolean markProcessedIfAbsent(String handlerKey, UUID eventId) { return processed.add(handlerKey + ":" + eventId); }
        };

        DomainEventPublisher publisher = new DomainEventPublisher() {
            @Override public void publish(DomainEvent event) { published.add(event); }
            @Override public void publish(Collection<? extends DomainEvent> events) { published.addAll(events); }
        };

        handler = new CorrectAppliedDrawResultCommandHandler(
            drawLookupPort,
            drawLifecyclePort,
            salesGuard,
            drawChannelCatalog,
            processedEventPort,
            publisher,
            UUID::randomUUID,
            Clock.systemUTC()
        );
    }

    @Test
    @DisplayName("rejects too short reason")
    void rejectsTooShortReason() {
        var command = new CorrectAppliedDrawResultCommand(drawId, correctedResultId, "too short", "k1", false);

        assertThatThrownBy(() -> handler.handle(command))
            .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("rejects when corrected result equals previous")
    void rejectsSameResult() {
        var command = new CorrectAppliedDrawResultCommand(drawId, previousResultId, "valid reason text", "k2", false);

        assertThatThrownBy(() -> handler.handle(command))
            .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("publishes corrected event and saves draw")
    void publishesEvent() {
        var command = new CorrectAppliedDrawResultCommand(drawId, correctedResultId, "valid reason text", "k3", false);

        handler.handle(command);

        assertThat(saveCalls).isEqualTo(1);
        assertThat(published).hasSize(1);
        assertThat(published.getFirst()).isInstanceOf(DrawResultCorrectedEvent.class);
    }

    private Draw resultedDraw(DrawResultId resultId) {
        var now = Instant.now();
        return new Draw(
            drawId,
            tenantId,
            drawChannelId,
            LocalDate.now(),
            now.plusSeconds(3600),
            now.plusSeconds(1800),
            DrawStatus.RESULTED,
            resultId,
            now.minusSeconds(3600),
            now.minusSeconds(1800),
            now.minusSeconds(1200),
            null,
            null,
            null,
            DrawSource.SYSTEM,
            null,
            null,
            false,
            true
        );
    }
}

