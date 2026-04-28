package com.tchalanet.server.core.outlet.application.command.handler;

import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.core.outlet.application.command.model.CloseDayMode;
import com.tchalanet.server.core.outlet.application.command.model.CloseOutletDayCommand;
import com.tchalanet.server.core.outlet.application.command.model.CloseOutletDayPayload;
import com.tchalanet.server.core.outlet.application.port.out.OutletReaderPort;
import com.tchalanet.server.core.outlet.application.port.out.OutletWriterPort;
import com.tchalanet.server.core.outlet.application.port.out.SalesTicketAdminPort;
import com.tchalanet.server.core.outlet.application.port.out.SessionAdminPort;
import com.tchalanet.server.core.outlet.domain.event.OutletDayClosedEvent;
import com.tchalanet.server.core.outlet.domain.model.Outlet;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

@UseCase
@RequiredArgsConstructor
public class CloseOutletDayCommandHandler implements VoidCommandHandler<CloseOutletDayCommand> {

    private final SessionAdminPort sessionAdmin;
    private final SalesTicketAdminPort salesAdmin;
    private final OutletReaderPort outletReader;
    private final OutletWriterPort outletWriter;
    private final DomainEventPublisher publisher;
    private final IdGenerator idGenerator;
    private final Clock clock;

    @Override
    @TchTx
    public void handle(CloseOutletDayCommand cmd) {
        var tenantId = cmd.tenantId();
        var outletId = cmd.outletId();

        var payload = cmd.payload();
        if (payload == null)
            payload =
                new CloseOutletDayPayload(LocalDate.now(), LocalDate.now(), CloseDayMode.STRICT, null);

        var mode = payload.getMode() == null ? CloseDayMode.STRICT : payload.getMode();

        boolean hasOpen = sessionAdmin.hasOpenSessions(outletId);
        if (hasOpen) {
            if (mode == CloseDayMode.STRICT) {
                throw new IllegalStateException("Cannot close day: open sessions exist");
            }
            // FORCE_SESSIONS or FORCE_ALL will proceed to close
            sessionAdmin.closeAllOpenSessions(outletId, "closed_by_outlet_day");
        }

        // compute date range instants
        LocalDate fromDate = payload.getFrom() == null ? LocalDate.now() : payload.getFrom();
        LocalDate toDate = payload.getTo() == null ? LocalDate.now() : payload.getTo();
        Instant from = fromDate.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant to = toDate.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        var stats = salesAdmin.getCloseStats(outletId, from, to);

        // pending is defined as SOLD tickets
        long pending = stats.sold();
        if (pending > 0 && mode != CloseDayMode.FORCE_ALL) {
            throw new IllegalStateException("Cannot close day: pending sold tickets exist: " + pending);
        }

        // refuse new tickets during closure (may be no-op for now)
        salesAdmin.refuseNewTickets(outletId);

        // mark outlet closed in domain and persist (also block sales)
        Outlet outlet = outletReader.getRequired(outletId);
        String reason =
            payload.getReason() == null || payload.getReason().isBlank()
                ? "closed_by_outlet_day"
                : payload.getReason();
        Outlet updated = outlet.closeDay().withSalesBlocked(true, reason, Instant.now(clock));
        outletWriter.save(updated);
        LocalDate closedDate = payload.getTo() == null ? toDate : payload.getTo();
        var event =
            new OutletDayClosedEvent(
                EventId.of(idGenerator.newUuid()), Instant.now(clock), tenantId, outletId, closedDate);
        AfterCommit.run(() -> publisher.publish(event));
    }
}
