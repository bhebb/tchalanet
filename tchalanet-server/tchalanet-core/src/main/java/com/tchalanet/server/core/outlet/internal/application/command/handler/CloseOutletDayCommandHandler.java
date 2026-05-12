package com.tchalanet.server.core.outlet.internal.application.command.handler;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.core.outlet.api.command.CloseDayMode;
import com.tchalanet.server.core.outlet.api.command.CloseOutletDayCommand;
import com.tchalanet.server.core.outlet.api.command.CloseOutletDayPayload;
import com.tchalanet.server.core.outlet.internal.application.port.out.OutletReaderPort;
import com.tchalanet.server.core.outlet.internal.application.port.out.OutletWriterPort;
import com.tchalanet.server.core.outlet.internal.application.port.out.SalesTicketAdminPort;
import com.tchalanet.server.core.outlet.internal.domain.event.OutletDayClosedEvent;
import com.tchalanet.server.core.session.api.query.GetOpenedSalesSessionQuery;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;


@UseCase
@RequiredArgsConstructor
public class CloseOutletDayCommandHandler implements VoidCommandHandler<CloseOutletDayCommand> {

    private final SalesTicketAdminPort salesAdmin;
    private final OutletReaderPort outletReader;
    private final OutletWriterPort outletWriter;
    private final DomainEventPublisher publisher;
    private final IdGenerator idGenerator;
    private final Clock clock;
    private final QueryBus queryBus;

    @Override
    @TchTx
    public void handle(CloseOutletDayCommand cmd) {
        var tenantId = cmd.tenantId();
        var outletId = cmd.outletId();
        var now = Instant.now(clock);

        var payload = normalizePayload(cmd.payload());
        var mode = payload.getMode();

        var outlet = outletReader.getRequired(outletId);
        var zone = ZoneId.of(outlet.timezone());

        var openSessions =
            queryBus.ask(new GetOpenedSalesSessionQuery(tenantId, outletId, null, null));

        if (!openSessions.isEmpty() && mode == CloseDayMode.STRICT) {
            throw new IllegalStateException("Cannot close outlet day: open sales sessions exist");
        }

        var from = payload.getFrom().atStartOfDay(zone).toInstant();
        var to = payload.getTo().plusDays(1).atStartOfDay(zone).toInstant();

        var stats = salesAdmin.getCloseStats(outletId, from, to);

        if (stats.sold() > 0 && mode != CloseDayMode.FORCE_ALL) {
            throw new IllegalStateException(
                "Cannot close outlet day: pending sold tickets exist: " + stats.sold());
        }

        var reason =
            payload.getReason() == null || payload.getReason().isBlank()
                ? "closed_by_outlet_day"
                : payload.getReason();

        var updated = outlet.closeDay().blockSales(reason, now);

        if (updated.equals(outlet)) {
            return;
        }

        outletWriter.save(updated);

        var event =
            new OutletDayClosedEvent(
                EventId.of(idGenerator.newUuid()),
                now,
                tenantId,
                outletId,
                payload.getTo(),
                mode,
                cmd.actorUserId());

        AfterCommit.run(() -> publisher.publish(event));
    }

    private CloseOutletDayPayload normalizePayload(CloseOutletDayPayload payload) {
        var today = LocalDate.now(clock);

        if (payload == null) {
            return new CloseOutletDayPayload(today, today, CloseDayMode.STRICT, null);
        }

        var from = payload.getFrom() == null ? today : payload.getFrom();
        var to = payload.getTo() == null ? from : payload.getTo();
        var mode = payload.getMode() == null ? CloseDayMode.STRICT : payload.getMode();

        if (to.isBefore(from)) {
            throw new IllegalArgumentException("Close day 'to' date cannot be before 'from' date");
        }

        return new CloseOutletDayPayload(from, to, mode, payload.getReason());
    }
}
