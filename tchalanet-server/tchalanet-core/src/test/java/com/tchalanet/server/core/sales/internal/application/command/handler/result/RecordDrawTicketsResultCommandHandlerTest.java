package com.tchalanet.server.core.sales.internal.application.command.handler.result;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.event.DomainEvent;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.DrawResultId;
import com.tchalanet.server.common.types.id.ResultSlotId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.core.drawresult.internal.application.port.out.DrawResultProjection;
import com.tchalanet.server.core.sales.api.command.result.RecordDrawTicketsResultCommand;
import com.tchalanet.server.core.sales.internal.application.port.out.TicketReaderPort;
import com.tchalanet.server.core.sales.internal.application.port.out.TicketWriterPort;
import com.tchalanet.server.core.sales.internal.application.service.result.TicketWinningCalculator;
import com.tchalanet.server.core.sales.internal.domain.model.ticket.Ticket;
import com.tchalanet.server.core.sales.internal.infra.cache.SalesTicketCacheEvictor;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.cache.support.NoOpCacheManager;

class RecordDrawTicketsResultCommandHandlerTest {

    @Test
    void drawWithNoTicketsSettlesToZeroTicketUpdates() {
        var tenantId = TenantId.of(UUID.randomUUID());
        var drawId = DrawId.of(UUID.randomUUID());
        var drawResultId = DrawResultId.of(UUID.randomUUID());
        var handler = new RecordDrawTicketsResultCommandHandler(
            new EmptyTicketReaderPort(),
            new FailingTicketWriterPort(),
            new ProjectionQueryBus(drawResultId),
            new TicketWinningCalculator(),
            new FailingEventPublisher(),
            new SalesTicketCacheEvictor(new NoOpCacheManager()),
            () -> UUID.randomUUID(),
            Clock.fixed(Instant.parse("2026-01-02T22:00:00Z"), ZoneOffset.UTC));

        var result = handler.handle(new RecordDrawTicketsResultCommand(
            tenantId,
            drawId,
            drawResultId,
            LocalDate.of(2026, 1, 2),
            ResultSlotId.of(UUID.randomUUID()),
            DrawChannelId.of(UUID.randomUUID())));

        assertThat(result.processedTickets()).isZero();
        assertThat(result.updatedTickets()).isZero();
        assertThat(result.skippedTickets()).isZero();
    }

    private record ProjectionQueryBus(DrawResultId drawResultId) implements QueryBus {
        @Override
        @SuppressWarnings("unchecked")
        public <R> R ask(Query<R> query) {
            return (R) new DrawResultProjection(
                drawResultId,
                "MIDDAY",
                Instant.parse("2026-01-02T22:00:00Z"),
                "123",
                "45",
                "67",
                null,
                List.of("45", "67"));
        }
    }

    private static final class EmptyTicketReaderPort implements TicketReaderPort {
        @Override public Optional<Ticket> findById(TicketId ticketId) { return Optional.empty(); }
        @Override public Ticket getRequired(TicketId ticketId) { throw new AssertionError("No ticket expected"); }
        @Override public Optional<Ticket> findByTicketCode(String ticketCode) { return Optional.empty(); }
        @Override public Optional<Ticket> findByPublicCode(String publicCode) { return Optional.empty(); }
        @Override public Optional<Ticket> findByVerificationCode(String verificationCode) { return Optional.empty(); }
        @Override public List<Ticket> findByDrawId(DrawId drawId) { return List.of(); }
    }

    private static final class FailingTicketWriterPort implements TicketWriterPort {
        @Override public Ticket save(Ticket ticket) { throw new AssertionError("No ticket should be saved"); }
        @Override public void flushPending() { throw new AssertionError("No flush expected"); }
    }

    private static final class FailingEventPublisher implements DomainEventPublisher {
        @Override public void publish(DomainEvent event) { throw new AssertionError("No event expected"); }
        @Override public void publish(Collection<? extends DomainEvent> events) {
            if (!events.isEmpty()) {
                throw new AssertionError("No event expected");
            }
        }
    }
}
